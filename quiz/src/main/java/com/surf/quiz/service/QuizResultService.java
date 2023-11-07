package com.surf.quiz.service;


import com.surf.quiz.dto.MemberDto;
import com.surf.quiz.dto.QuestionDto;
import com.surf.quiz.dto.QuestionResultDto;
import com.surf.quiz.entity.Quiz;
import com.surf.quiz.entity.QuizResult;
import com.surf.quiz.entity.QuizRoom;
import com.surf.quiz.repository.QuizRepository;
import com.surf.quiz.repository.QuizResultRepository;
import com.surf.quiz.repository.QuizRoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class QuizResultService {

    private final QuizRepository quizRepository;
    private final SimpMessagingTemplate messageTemplate;
    private final QuizResultRepository quizResultRepository;
    private final QuizRoomRepository quizRoomRepository;


    public QuizResultService(QuizRepository quizRepository, SimpMessagingTemplate messageTemplate, QuizResultRepository quizResultRepository, QuizRoomRepository quizRoomRepository) {
        this.quizRepository = quizRepository;
        this.messageTemplate = messageTemplate;
        this.quizResultRepository = quizResultRepository;
        this.quizRoomRepository = quizRoomRepository;
    }



    // 퀴즈 완료
    @Transactional
    public void completeQuiz(String roomId) {
        Quiz quiz = findQuizByRoomId(roomId);
        if (quiz.isComplete()) {
            return;
        }
        QuizRoom quizroom = getQuizRoomById(roomId);

        // 정답 대조
        processUserAnswers(quiz, quizroom);
        // 결과 저장
        saveAndSendResults(roomId, quiz);
    }

    private Quiz findQuizByRoomId(String roomId) {
        return quizRepository.findByRoomId(Integer.parseInt(roomId))
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
    }

    private QuizRoom getQuizRoomById(String roomId) {
        return quizRoomRepository.findById(Long.valueOf(roomId))
                .orElseThrow(() -> new IllegalArgumentException("Invalid roomId: " + roomId));
    }

    // 정답 처리
    private void processUserAnswers(Quiz quiz, QuizRoom quizroom) {
        int userAnswersSize = getUserAnswersSize(quiz);
        System.out.println("userAnswersSize = " + userAnswersSize);
        for (MemberDto user : quizroom.getUsers()) {
            // 유저 정답 리스트 가져오기
            Map<Integer, String> userAnswerList = getUserAnswerList(quiz, user, userAnswersSize);
            createQuizResultAndSave(user, quiz, userAnswerList);
        }
    }

    private Map<Integer, String> getUserAnswerList(Quiz quiz, MemberDto user, int userAnswersSize) {
        if (userAnswersSize == 0 || userAnswersSize != quiz.getUserCnt()) {
            return quiz.getUserAnswers().get(String.valueOf(user.getUserPk()));
        } else {
            return quiz.getUserAnswers().entrySet().stream()
                    .filter(entry -> user.getUserPk() == Long.parseLong(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
    }

    private int getUserAnswersSize(Quiz quiz) {
        return Optional.ofNullable(quiz.getUserAnswers())
                .map(Map::size)
                .orElse(0);
    }

    // 문제 결과 만들고 저장
    private void createQuizResultAndSave(MemberDto user, Quiz quiz, Map<Integer, String> userAnswerList) {
        QuizResult quizResult = createQuizResult(user, quiz);
        List<QuestionResultDto> questionResults = createQuestionResults(quiz, userAnswerList, quizResult);
        quizResult.setQuestionResult(questionResults);
        quizResultRepository.save(quizResult);
    }

    // 개인 결과
    private QuizResult createQuizResult(MemberDto user, Quiz quiz) {
        QuizResult quizResult = new QuizResult();
        quizResult.setQuizId(quiz.getId());
        quizResult.setRoomId(quiz.getRoomId());
        quizResult.setRoomName(quiz.getRoomName());
        quizResult.setUser(user);
        quizResult.setTotals(quiz.getQuestion().size());
        String formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        quizResult.setExamDone(formattedDateTime);
        quizResult.setExamStart(quiz.getCreatedDate());

        return quizResult;
    }

    // 퀴즈 결과 만들기
    private List<QuestionResultDto> createQuestionResults(Quiz quiz, Map<Integer, String> userAnswerList, QuizResult quizResult) {
        int correctCount = 0;
        List<QuestionResultDto> questionResults = new ArrayList<>();
        for (int i = 0; i < quiz.getQuestion().size(); i++) {
            QuestionDto questionDto = quiz.getQuestion().get(i);
            QuestionResultDto questionResult = createQuestionResult(questionDto, userAnswerList, i);
            questionResults.add(questionResult);
            if (isCorrectAnswer(questionDto, questionResult)) {
                correctCount++;
            }
        }
        quizResult.setCorrect(correctCount);
        return questionResults;
    }

    // 문제 결과
    private QuestionResultDto createQuestionResult(QuestionDto questionDto, Map<Integer, String> userAnswerList, int index) {
        QuestionResultDto questionResult = new QuestionResultDto();
        questionResult.setId(questionDto.getId());
        questionResult.setQuestion(questionDto.getQuestion());
        questionResult.setExample(questionDto.getExample());
        questionResult.setAnswer(questionDto.getAnswer());
        String myAnswer = (userAnswerList != null && userAnswerList.get(index+1) != null) ? userAnswerList.get(index+1) : "0";
        questionResult.setMyAnswer(myAnswer);
        questionResult.setCommentary(questionDto.getCommentary());
        return questionResult;
    }

    // 정답이 맞는지 확인
    private boolean isCorrectAnswer(QuestionDto questionDto, QuestionResultDto questionResult) {
        return questionResult.getMyAnswer().equals(questionDto.getAnswer());
    }

    // 각 유저의 답안이 저장 > 결과 전송
    private void saveAndSendResults(String roomId, Quiz quiz) {
        System.out.println("111roomId = " + roomId);
        if (quizResultRepository.countByRoomId(Integer.parseInt(roomId)) == quiz.getUserCnt()) {
            quiz.setComplete(true);
            quizRepository.save(quiz);

            List<QuizResult> results = quizResultRepository.findByRoomId(Integer.parseInt(roomId));
            results.sort((o1, o2) -> o2.getCorrect() - o1.getCorrect());

            // 각 QuizResult에서 필요한 정보를 추출하여 ranking 리스트 생성
            List<Map<String, Object>> ranking = new ArrayList<>();
            int rank = 1;
            int prevCorrect = -1;
            for (QuizResult result : results) {
                Map<String, Object> userRanking = new HashMap<>();
                userRanking.put("userPk", result.getUser().getUserPk());
                userRanking.put("userName", result.getUser().getUserName()); // userName 필드 가정
                userRanking.put("userImg", result.getUser().getUserImg()); // userImg 필드 가정
                // 이전 점수와 현재 점수를 비교
                if (result.getCorrect() != prevCorrect) {
                    rank = ranking.size() + 1; // 이전 점수와 현재 점수가 다르면, 현재 등수를 ranking 리스트의 크기 + 1로 설정
                }
                userRanking.put("ranking", rank);
                prevCorrect = result.getCorrect();
                userRanking.put("correct", result.getCorrect());
                userRanking.put("total", result.getTotals()); // total 필드 가정
                ranking.add(userRanking);
            }

            // results와 ranking을 하나의 객체에 담아 보냄
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "result");
            payload.put("ranking", ranking);
            payload.put("result", results);
            System.out.println("payload = " + payload);

            messageTemplate.convertAndSend("/sub/quiz/" + roomId, payload);
        }
    }
}