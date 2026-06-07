package com.example.plog.ui.diary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.plog.R;
import com.example.plog.data.DiaryEmojiDecoration;
import com.example.plog.data.DiaryEntry;
import com.example.plog.data.DiaryInteractionRepository;
import com.example.plog.data.DiaryLineComment;
import com.example.plog.data.DiaryRepository;
import com.example.plog.databinding.FragmentDiaryDetailBinding;
import com.example.plog.network.RetrofitClient;
import com.example.plog.network.api.ExchangeDiaryApi;
import com.example.plog.network.dto.ExchangeDiaryResponse;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiaryDetailFragment extends Fragment {
    private static final String[] EMOJI_PALETTE = {"😊", "⭐", "❤️", "🌿", "✨", "👍"};
    private static final String CURRENT_USER_NAME = "나";

    private FragmentDiaryDetailBinding binding;
    private DiaryRepository diaryRepository;
    private DiaryInteractionRepository interactionRepository;
    private String diaryDate;
    private DiaryEntry diaryEntry;
    private boolean interactionsEnabled;
    private String editingCommentId;
    private String pendingDeleteCommentId;

    // 교환일기 모드
    private boolean isExchange = false;
    private Long exchangeDiaryId = null;
    private String exchangeContent = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDiaryDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        diaryRepository = new DiaryRepository(requireContext());
        interactionRepository = new DiaryInteractionRepository(requireContext());
        diaryDate = todayKey();

        if (getArguments() != null) {
            isExchange = getArguments().getBoolean("isExchange", false);
            long did = getArguments().getLong("diaryId", -1L);
            if (did != -1L) exchangeDiaryId = did;
        }

        if (isExchange && exchangeDiaryId != null) {
            loadExchangeDiary();
        } else {
            loadDiary();
        }
        setupListeners();
    }

    private void loadExchangeDiary() {
        ExchangeDiaryApi api = RetrofitClient.getClient().create(ExchangeDiaryApi.class);
        api.getDiary(exchangeDiaryId).enqueue(new Callback<ExchangeDiaryResponse>() {
            @Override
            public void onResponse(Call<ExchangeDiaryResponse> call, Response<ExchangeDiaryResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    ExchangeDiaryResponse diary = response.body();
                    exchangeContent = diary.getContent();
                    interactionsEnabled = true;

                    binding.tvDate.setText(diary.getCreatedAt() != null ? diary.getCreatedAt().substring(0, 10) : "");
                    binding.tvWeather.setText("");
                    binding.tvLocation.setText("");
                    binding.tvTitle.setText("DAY " + diary.getDayNumber());
                    binding.tvSecretNotice.setVisibility(View.GONE);
                    binding.btnEmoji.setVisibility(View.VISIBLE);
                    binding.btnEdit.setVisibility(View.GONE);

                    renderExchangeLines();
                    renderEmojiPalette();
                    binding.diaryCanvas.post(() -> {
                        syncEmojiLayerHeight();
                        renderDecorations();
                    });
                }
            }
            @Override
            public void onFailure(Call<ExchangeDiaryResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "일기를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderExchangeLines() {
        binding.lineContainer.removeAllViews();
        if (exchangeContent == null) return;
        String[] lines = exchangeContent.split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            binding.lineContainer.addView(createLineView(lines[i], i));
        }
        binding.lineContainer.post(this::syncEmojiLayerHeight);
    }

    private void loadDiary() {
        diaryEntry = diaryRepository.getDiary(diaryDate);
        if (diaryEntry == null) {
            Toast.makeText(requireContext(), "작성된 일기가 없습니다.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(binding.getRoot()).navigate(R.id.action_diaryDetailFragment_to_diaryEditFragment);
            return;
        }

        interactionsEnabled = !diaryEntry.isSecret();
        binding.tvDate.setText(formatDisplayDate(diaryEntry.getDate()));
        binding.tvWeather.setText(blankToDefault(diaryEntry.getWeather(), "맑음"));
        binding.tvLocation.setText(blankToDefault(diaryEntry.getLocation(), "위치 정보 없음"));
        binding.tvTitle.setText("제목: " + diaryEntry.getTitle());
        binding.tvSecretNotice.setVisibility(interactionsEnabled ? View.GONE : View.VISIBLE);
        binding.btnEmoji.setVisibility(interactionsEnabled ? View.VISIBLE : View.GONE);

        renderRepresentativePhoto();
        renderLines();
        renderEmojiPalette();
        binding.diaryCanvas.post(() -> {
            syncEmojiLayerHeight();
            renderDecorations();
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        if (!isExchange) {
            binding.btnEdit.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.action_diaryDetailFragment_to_diaryEditFragment));
        }
        binding.btnEmoji.setOnClickListener(v -> {
            boolean show = binding.emojiPalette.getVisibility() != View.VISIBLE;
            binding.emojiPalette.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.btnEmoji.setText(show ? "×" : "+");
        });
        binding.deleteSheetScrim.setOnClickListener(v -> hideDeleteSheet());
        binding.deleteSheet.setOnClickListener(v -> {});
        binding.btnCancelDelete.setOnClickListener(v -> hideDeleteSheet());
        binding.btnConfirmDelete.setOnClickListener(v -> deletePendingComment());
    }

    private void renderRepresentativePhoto() {
        String photoUri = diaryEntry.getRepresentativePhotoUri();
        if (photoUri == null || photoUri.isEmpty()) {
            binding.ivRepresentativePhoto.setImageDrawable(null);
            return;
        }

        Uri uri = Uri.parse(photoUri);
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                binding.ivRepresentativePhoto.setImageBitmap(bitmap);
                return;
            }
        } catch (Exception ignored) {
        }

        Glide.with(this)
                .load(uri)
                .centerCrop()
                .error(R.drawable.bg_photo_placeholder)
                .into(binding.ivRepresentativePhoto);
    }

    private void renderLines() {
        binding.lineContainer.removeAllViews();
        String[] lines = diaryEntry.getBody().split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            binding.lineContainer.addView(createLineView(lines[i], i));
        }
        binding.lineContainer.post(this::syncEmojiLayerHeight);
    }

    private View createLineView(String line, int lineIndex) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(4), dp(7), dp(4), dp(10));

        TextView lineText = new TextView(requireContext());
        lineText.setText(line.isEmpty() ? " " : line);
        lineText.setTextColor(requireContext().getColor(R.color.text_primary));
        lineText.setTextSize(14);
        lineText.setMinHeight(dp(30));
        row.addView(lineText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView divider = new TextView(requireContext());
        divider.setText("");
        divider.setBackgroundColor(requireContext().getColor(R.color.diary_line));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        );
        dividerParams.setMargins(0, dp(2), 0, dp(6));
        row.addView(divider, dividerParams);

        addInlineComments(row, lineIndex);
        if (interactionsEnabled) {
            row.addView(createInlineCommentInput(lineIndex));
        }
        return row;
    }

    private void addInlineComments(LinearLayout row, int lineIndex) {
        String key = isExchange ? "exchange_" + exchangeDiaryId : diaryDate;
        List<DiaryLineComment> comments = interactionRepository.getComments(key, lineIndex);
        if (!comments.isEmpty()) {
            TextView label = new TextView(requireContext());
            label.setText("댓글 " + comments.size());
            label.setTextColor(requireContext().getColor(R.color.gray_400));
            label.setTextSize(10);
            label.setPadding(dp(8), 0, 0, dp(2));
            row.addView(label);
        }

        for (DiaryLineComment comment : comments) {
            if (comment.getId().equals(editingCommentId)) {
                row.addView(createInlineCommentEditor(comment));
                continue;
            }

            LinearLayout commentBox = new LinearLayout(requireContext());
            commentBox.setOrientation(LinearLayout.VERTICAL);
            commentBox.setBackgroundResource(R.drawable.bg_comment_chip);
            commentBox.setPadding(dp(8), dp(4), dp(8), dp(4));

            TextView author = new TextView(requireContext());
            author.setText(comment.getAuthorName());
            author.setTextColor(requireContext().getColor(R.color.gray_700));
            author.setTextSize(10);
            commentBox.addView(author);

            TextView content = new TextView(requireContext());
            content.setText(comment.getContent());
            content.setTextColor(requireContext().getColor(R.color.text_primary));
            content.setTextSize(12);
            content.setPadding(0, dp(1), 0, 0);
            commentBox.addView(content);

            LinearLayout actionRow = new LinearLayout(requireContext());
            actionRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setPadding(0, dp(2), 0, 0);

            if (CURRENT_USER_NAME.equals(comment.getAuthorName())) {
                TextView edit = createCommentAction("수정");
                edit.setOnClickListener(v -> {
                    editingCommentId = comment.getId();
                    if (isExchange) renderExchangeLines();
                    else renderLines();
                });
                actionRow.addView(edit);
            }

            TextView delete = createCommentAction("삭제");
            delete.setOnClickListener(v -> confirmDeleteComment(comment.getId()));
            actionRow.addView(delete);
            commentBox.addView(actionRow);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dp(12), 0, 0, dp(4));
            row.addView(commentBox, params);
        }
    }

    private View createInlineCommentInput(int lineIndex) {
        String key = isExchange ? "exchange_" + exchangeDiaryId : diaryDate;
        LinearLayout inputRow = new LinearLayout(requireContext());
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(dp(12), dp(1), 0, 0);

        EditText input = new EditText(requireContext());
        input.setHint("댓글을 입력하세요");
        input.setSingleLine(true);
        input.setTextSize(11);
        input.setBackgroundResource(R.drawable.bg_diary_field);
        input.setPadding(dp(8), 0, dp(8), 0);
        inputRow.addView(input, new LinearLayout.LayoutParams(0, dp(30), 1));

        TextView submit = new TextView(requireContext());
        submit.setText("등록");
        submit.setGravity(Gravity.CENTER);
        submit.setTextColor(requireContext().getColor(R.color.white));
        submit.setTextSize(11);
        submit.setBackgroundResource(R.drawable.bg_black_round);
        submit.setOnClickListener(v -> {
            String content = input.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "댓글 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            interactionRepository.addComment(key, lineIndex, content);
            if (isExchange) renderExchangeLines();
            else renderLines();
        });
        LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(dp(44), dp(28));
        submitParams.setMarginStart(dp(5));
        inputRow.addView(submit, submitParams);

        return inputRow;
    }

    private View createInlineCommentEditor(DiaryLineComment comment) {
        String key = isExchange ? "exchange_" + exchangeDiaryId : diaryDate;
        LinearLayout editor = new LinearLayout(requireContext());
        editor.setGravity(Gravity.CENTER_VERTICAL);
        editor.setOrientation(LinearLayout.HORIZONTAL);
        editor.setPadding(dp(12), dp(1), 0, dp(4));

        EditText input = new EditText(requireContext());
        input.setSingleLine(true);
        input.setText(comment.getContent());
        input.setSelection(input.getText().length());
        input.setTextSize(11);
        input.setBackgroundResource(R.drawable.bg_diary_field);
        input.setPadding(dp(8), 0, dp(8), 0);
        editor.addView(input, new LinearLayout.LayoutParams(0, dp(30), 1));

        TextView save = createCompactButton("저장", true);
        save.setOnClickListener(v -> {
            String content = input.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "댓글 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            interactionRepository.updateComment(key, comment.getId(), content);
            editingCommentId = null;
            if (isExchange) renderExchangeLines();
            else renderLines();
        });
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(dp(44), dp(28));
        saveParams.setMarginStart(dp(5));
        editor.addView(save, saveParams);

        TextView cancel = createCompactButton("취소", false);
        cancel.setOnClickListener(v -> {
            editingCommentId = null;
            if (isExchange) renderExchangeLines();
            else renderLines();
        });
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(dp(44), dp(28));
        cancelParams.setMarginStart(dp(5));
        editor.addView(cancel, cancelParams);

        return editor;
    }

    private TextView createCommentAction(String label) {
        TextView action = new TextView(requireContext());
        action.setText(label);
        action.setGravity(Gravity.CENTER);
        action.setTextColor(requireContext().getColor(R.color.gray_700));
        action.setTextSize(10);
        action.setPadding(dp(6), 0, dp(6), 0);
        return action;
    }

    private TextView createCompactButton(String label, boolean primary) {
        TextView button = new TextView(requireContext());
        button.setText(label);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(11);
        button.setTextColor(requireContext().getColor(primary ? R.color.white : R.color.gray_700));
        button.setBackgroundResource(primary ? R.drawable.bg_black_round : R.drawable.bg_comment_chip);
        return button;
    }

    private void confirmDeleteComment(String commentId) {
        pendingDeleteCommentId = commentId;
        binding.deleteSheetScrim.setVisibility(View.VISIBLE);
        binding.deleteSheet.setTranslationY(dp(120));
        binding.deleteSheet.animate().translationY(0).setDuration(160).start();
    }

    private void hideDeleteSheet() {
        pendingDeleteCommentId = null;
        binding.deleteSheet.animate()
                .translationY(dp(120))
                .setDuration(140)
                .withEndAction(() -> {
                    if (binding != null) {
                        binding.deleteSheetScrim.setVisibility(View.GONE);
                        binding.deleteSheet.setTranslationY(0);
                    }
                })
                .start();
    }

    private void deletePendingComment() {
        if (pendingDeleteCommentId == null) {
            hideDeleteSheet();
            return;
        }
        String key = isExchange ? "exchange_" + exchangeDiaryId : diaryDate;
        String commentId = pendingDeleteCommentId;
        interactionRepository.deleteComment(key, commentId);
        if (commentId.equals(editingCommentId)) {
            editingCommentId = null;
        }
        pendingDeleteCommentId = null;
        binding.deleteSheetScrim.setVisibility(View.GONE);
        if (isExchange) renderExchangeLines();
        else renderLines();
        Toast.makeText(requireContext(), "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void renderEmojiPalette() {
        binding.emojiPalette.removeAllViews();
        for (String emoji : EMOJI_PALETTE) {
            TextView emojiButton = new TextView(requireContext());
            emojiButton.setText(emoji);
            emojiButton.setGravity(Gravity.CENTER);
            emojiButton.setTextSize(24);
            emojiButton.setOnClickListener(v -> addEmojiDecoration(emoji));
            binding.emojiPalette.addView(emojiButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
    }

    private void addEmojiDecoration(String emoji) {
        String key = isExchange ? "exchange_" + exchangeDiaryId : diaryDate;
        DiaryEmojiDecoration decoration = interactionRepository.addDecoration(key, emoji);
        binding.emojiPalette.setVisibility(View.GONE);
        binding.btnEmoji.setText("+");
        binding.diaryCanvas.post(() -> {
            syncEmojiLayerHeight();
            addDecorationView(decoration);
        });
    }

    private void renderDecorations() {
        String key = isExchange ? "exchange_" + exchangeDiaryId : diaryDate;
        binding.emojiLayer.removeAllViews();
        for (DiaryEmojiDecoration decoration : interactionRepository.getDecorations(key)) {
            addDecorationView(decoration);
        }
    }

    private void addDecorationView(DiaryEmojiDecoration decoration) {
        syncEmojiLayerHeight();
        TextView emojiView = new TextView(requireContext());
        emojiView.setText(decoration.getEmoji());
        emojiView.setTextSize(30 * decoration.getScale());
        emojiView.setGravity(Gravity.CENTER);
        emojiView.setTag(decoration);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(54), dp(54));
        int layerWidth = Math.max(1, binding.emojiLayer.getWidth());
        int layerHeight = Math.max(dp(320), binding.emojiLayer.getHeight());
        params.leftMargin = (int) (decoration.getXRatio() * Math.max(1, layerWidth - dp(54)));
        params.topMargin = (int) (decoration.getYRatio() * Math.max(1, layerHeight - dp(54)));
        binding.emojiLayer.addView(emojiView, params);
        binding.emojiLayer.bringToFront();

        emojiView.setOnTouchListener(new EmojiDragListener());
    }

    private void syncEmojiLayerHeight() {
        int targetHeight = Math.max(dp(320), binding.lineContainer.getHeight() + dp(20));
        ViewGroup.LayoutParams canvasParams = binding.diaryCanvas.getLayoutParams();
        if (canvasParams.height != targetHeight) {
            canvasParams.height = targetHeight;
            binding.diaryCanvas.setLayoutParams(canvasParams);
        }

        FrameLayout.LayoutParams layerParams = (FrameLayout.LayoutParams) binding.emojiLayer.getLayoutParams();
        if (layerParams.height != targetHeight) {
            layerParams.height = targetHeight;
            binding.emojiLayer.setLayoutParams(layerParams);
        }
    }

    private class EmojiDragListener implements View.OnTouchListener {
        private float downRawX;
        private float downRawY;
        private int startLeft;
        private int startTop;
        private boolean moved;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable longPressRunnable;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    disallowParentScroll(true);
                    moved = false;
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    FrameLayout.LayoutParams downParams = (FrameLayout.LayoutParams) view.getLayoutParams();
                    startLeft = downParams.leftMargin;
                    startTop = downParams.topMargin;
                    longPressRunnable = () -> deleteEmojiDecoration(view);
                    handler.postDelayed(longPressRunnable, 650);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    disallowParentScroll(true);
                    if (Math.abs(event.getRawX() - downRawX) > dp(2)
                            || Math.abs(event.getRawY() - downRawY) > dp(2)) {
                        moved = true;
                        if (longPressRunnable != null) {
                            handler.removeCallbacks(longPressRunnable);
                        }
                    }
                    FrameLayout.LayoutParams moveParams = (FrameLayout.LayoutParams) view.getLayoutParams();
                    int maxLeft = Math.max(0, binding.emojiLayer.getWidth() - view.getWidth());
                    int maxTop = Math.max(0, binding.emojiLayer.getHeight() - view.getHeight());
                    moveParams.leftMargin = clamp(startLeft + (int) (event.getRawX() - downRawX), 0, maxLeft);
                    moveParams.topMargin = clamp(startTop + (int) (event.getRawY() - downRawY), 0, maxTop);
                    view.setLayoutParams(moveParams);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    disallowParentScroll(false);
                    if (longPressRunnable != null) {
                        handler.removeCallbacks(longPressRunnable);
                    }
                    if (moved) {
                        saveDecorationPosition(view);
                    } else {
                        view.performClick();
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    private void disallowParentScroll(boolean disallow) {
        ViewParent parent = binding.diaryCanvas.getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
            parent = parent.getParent();
        }
    }

    private void deleteEmojiDecoration(View view) {
        String key = isExchange ? "exchange_" + exchangeDiaryId : diaryDate;
        DiaryEmojiDecoration target = (DiaryEmojiDecoration) view.getTag();
        interactionRepository.deleteDecoration(key, target.getId());
        binding.emojiLayer.removeView(view);
        Toast.makeText(requireContext(), "이모지가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void saveDecorationPosition(View view) {
        DiaryEmojiDecoration decoration = (DiaryEmojiDecoration) view.getTag();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        int maxLeft = Math.max(1, binding.emojiLayer.getWidth() - view.getWidth());
        int maxTop = Math.max(1, binding.emojiLayer.getHeight() - view.getHeight());
        decoration.setXRatio(params.leftMargin / (float) maxLeft);
        decoration.setYRatio(params.topMargin / (float) maxTop);
        interactionRepository.saveDecoration(decoration);
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
    }

    private String formatDisplayDate(String dateKey) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(dateKey);
            if (date != null) {
                return new SimpleDateFormat("yy/MM/dd(E)", Locale.KOREA).format(date);
            }
        } catch (ParseException ignored) {
        }
        return dateKey;
    }

    private String blankToDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}