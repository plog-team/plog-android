package com.example.plog.ui.aiguide;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.plog.R;
import com.example.plog.model.GuideQuestionDto;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewPager2 페이지당 1개 질문과 답변 카드 3장.
 * 카드 탭 시 입력 자동 채움 + 강조 + 다음 페이지 자동 이동.
 */
public class QuestionAnswerAdapter extends RecyclerView.Adapter<QuestionAnswerAdapter.QAH> {

    private final List<GuideQuestionDto> items = new ArrayList<>();
    private final Map<Long, String> answers = new HashMap<>();
    /** questionId → 선택된 카드 인덱스 (0/1/2). 직접 입력 시 -1. */
    private final Map<Long, Integer> selectedCardIdx = new HashMap<>();
    private ViewPager2 viewPager;

    public interface OnAnswerSelected {
        void onSelected(long questionId);
    }

    private OnAnswerSelected selectListener;

    public void setOnAnswerSelected(OnAnswerSelected l) {
        this.selectListener = l;
    }

    public void setViewPager(ViewPager2 vp) {
        this.viewPager = vp;
    }

    public void setItems(List<GuideQuestionDto> qs) {
        items.clear();
        if (qs != null) {
            items.addAll(qs);
            for (GuideQuestionDto q : qs) {
                if (q.answer != null && !q.answer.isEmpty()) {
                    answers.put(q.questionId, q.answer);
                }
            }
        }
        notifyDataSetChanged();
    }

    public Map<Long, String> getAnswers() {
        return new HashMap<>(answers);
    }

    public void addItem(GuideQuestionDto q) {
        if (q == null) return;
        items.add(q);
        if (q.answer != null && !q.answer.isEmpty()) {
            answers.put(q.questionId, q.answer);
        }
        notifyItemInserted(items.size() - 1);
    }

    @NonNull
    @Override
    public QAH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question_answer, parent, false);
        return new QAH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull QAH holder, int position) {
        GuideQuestionDto q = items.get(position);
        holder.bind(q, answers, selectedCardIdx, viewPager, getItemCount(), selectListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class QAH extends RecyclerView.ViewHolder {
        final TextView tvQuestion;
        final MaterialCardView[] cards = new MaterialCardView[3];
        final TextView[] tvAnswers = new TextView[3];
        final EditText etAnswer;
        TextWatcher currentWatcher;

        QAH(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            cards[0] = itemView.findViewById(R.id.cardAnswer1);
            cards[1] = itemView.findViewById(R.id.cardAnswer2);
            cards[2] = itemView.findViewById(R.id.cardAnswer3);
            tvAnswers[0] = itemView.findViewById(R.id.tvAnswer1);
            tvAnswers[1] = itemView.findViewById(R.id.tvAnswer2);
            tvAnswers[2] = itemView.findViewById(R.id.tvAnswer3);
            etAnswer = itemView.findViewById(R.id.etAnswer);
        }

        void bind(GuideQuestionDto q, Map<Long, String> answers,
                  Map<Long, Integer> selectedCardIdx,
                  ViewPager2 viewPager, int totalCount, OnAnswerSelected selectListener) {
            tvQuestion.setText("Q" + q.orderIdx + ". " + q.question);

            List<String> suggestions = q.suggestedAnswers;
            int selected = selectedCardIdx.getOrDefault(q.questionId, -1);
            for (int i = 0; i < 3; i++) {
                if (suggestions != null && i < suggestions.size() && suggestions.get(i) != null) {
                    cards[i].setVisibility(View.VISIBLE);
                    tvAnswers[i].setText(suggestions.get(i));
                    final int idx = i;
                    final String suggestion = suggestions.get(i);
                    applyCardStyle(cards[i], idx == selected);
                    cards[i].setOnClickListener(v -> {
                        etAnswer.setText(suggestion);
                        etAnswer.setSelection(etAnswer.getText().length());
                        answers.put(q.questionId, suggestion);
                        selectedCardIdx.put(q.questionId, idx);
                        // 모든 카드 스타일 갱신
                        for (int j = 0; j < 3; j++) {
                            applyCardStyle(cards[j], j == idx);
                        }
                        if (selectListener != null) {
                            selectListener.onSelected(q.questionId);
                        }
                    });
                } else {
                    cards[i].setVisibility(View.GONE);
                }
            }

            if (currentWatcher != null) etAnswer.removeTextChangedListener(currentWatcher);
            String existing = answers.getOrDefault(q.questionId, q.answer);
            etAnswer.setText(existing == null ? "" : existing);
            currentWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    String typed = s.toString();
                    answers.put(q.questionId, typed);
                    // 직접 입력하면 카드 강조 해제 (자유 입력으로 분류)
                    Integer selIdx = selectedCardIdx.get(q.questionId);
                    if (selIdx != null && selIdx >= 0) {
                        if (selIdx < tvAnswers.length && !typed.equals(tvAnswers[selIdx].getText().toString())) {
                            selectedCardIdx.put(q.questionId, -1);
                            for (int j = 0; j < 3; j++) applyCardStyle(cards[j], false);
                        }
                    }
                }
            };
            etAnswer.addTextChangedListener(currentWatcher);
        }

        /** 선택된 카드는 stroke + 배경 강조, 나머지는 기본 스타일. */
        private static void applyCardStyle(MaterialCardView card, boolean selected) {
            if (selected) {
                card.setStrokeWidth(dp(3));
                card.setStrokeColor(0xFF6750A4);
                card.setCardBackgroundColor(0xFFEDE7F6);
            } else {
                card.setStrokeWidth(0);
                card.setCardBackgroundColor(0xFFFAFAFA);
            }
        }

        private static int dp(int d) {
            return (int) (d * android.content.res.Resources.getSystem().getDisplayMetrics().density);
        }
    }
}
