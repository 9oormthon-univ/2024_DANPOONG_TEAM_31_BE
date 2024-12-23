package com.danpoong.withu.letter.service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import com.danpoong.withu.common.exception.ResourceNotFoundException;
import com.danpoong.withu.letter.controller.response.*;
import com.danpoong.withu.user.domain.User;
import com.danpoong.withu.schedule.domain.Schedule;
import com.danpoong.withu.letter.controller.response.LetterDatailResponse;
import com.danpoong.withu.letter.controller.response.LetterResponse;
import com.danpoong.withu.letter.controller.response.ScheduleLetterResponse;
import com.danpoong.withu.letter.domain.Letter;
import com.danpoong.withu.letter.domain.LetterType;
import com.danpoong.withu.letter.dto.LetterReqDto;
import com.danpoong.withu.letter.dto.ScheduleLetterRequestDto;
import com.danpoong.withu.letter.dto.TextLetterRequestDto;
import com.danpoong.withu.letter.repository.LetterRepository;
import com.danpoong.withu.schedule.domain.Schedule;
import com.danpoong.withu.schedule.repository.ScheduleRepository;
import com.danpoong.withu.user.domain.User;
import com.danpoong.withu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LetterServiceImpl implements LetterService {

  private final LetterRepository letterRepository;
  private final S3Presigner s3Presigner;

  private final UserRepository userRepository;
  private final ScheduleRepository scheduleRepository;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  @Override
  public Map<String, String> generatePresignedUrl(Long familyId, Long senderId, Long receiverId) {

    if (!userRepository.existsByIdAndFamily_FamilyId(senderId, familyId)) {
      throw new IllegalArgumentException(
          "Sender ID " + senderId + " is not a member of Family ID " + familyId);
    }
    if (!userRepository.existsByIdAndFamily_FamilyId(receiverId, familyId)) {
      throw new IllegalArgumentException(
          "Receiver ID " + receiverId + " is not a member of Family ID " + familyId);
    }

    String keyName =
        "letters/" + familyId + "/" + senderId + "-" + receiverId + "-" + UUID.randomUUID();

    PutObjectRequest objectRequest =
        PutObjectRequest.builder().bucket(bucketName).key(keyName).build();

    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10)) // URL 유효 시간 10분
            .putObjectRequest(objectRequest)
            .build();

    String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

    Map<String, String> response = new HashMap<>();
    response.put("presignedUrl", presignedUrl);
    response.put("keyName", keyName);
    return response;
  }

  @Override
  @Transactional
  public LetterResponse saveLetter(Long userId, LetterReqDto request) {

    if (!userRepository.existsByIdAndFamily_FamilyId(userId, request.getFamilyId())) {
      throw new IllegalArgumentException(
          "Sender ID " + userId + " is not a member of Family ID " + request.getFamilyId());
    }
    if (!userRepository.existsByIdAndFamily_FamilyId(
        request.getReceiverId(), request.getFamilyId())) {
      throw new IllegalArgumentException(
          "Receiver ID "
              + request.getReceiverId()
              + " is not a member of Family ID "
              + request.getFamilyId());
    }

    Letter letter =
        Letter.builder()
            .senderId(userId)
            .receiverId(request.getReceiverId())
            .familyId(request.getFamilyId())
            .scheduleId(request.getScheduleId())
            .letterType(request.getLetterType())
            .keyName(request.getKeyName())
            .textContent(request.getTextContent())
            .isLiked(false)
            .build();

    return new LetterResponse(letterRepository.save(letter));
  }

  @Override
  @Transactional
  public LetterResponse saveTextLetter(Long userId, TextLetterRequestDto request) {

    if (!userRepository.existsByIdAndFamily_FamilyId(userId, request.getFamilyId())) {
      throw new IllegalArgumentException(
          "Sender ID " + userId + " is not a member of Family ID " + request.getFamilyId());
    }

    if (!userRepository.existsByIdAndFamily_FamilyId(
        request.getReceiverId(), request.getFamilyId())) {
      throw new IllegalArgumentException(
          "Receiver ID "
              + request.getReceiverId()
              + " is not a member of Family ID "
              + request.getFamilyId());
    }

    Letter newLetter =
        Letter.builder()
            .senderId(userId)
            .receiverId(request.getReceiverId())
            .familyId(request.getFamilyId())
            .letterType(LetterType.TEXT)
            .textContent(request.getTextContent())
            .isLiked(false)
            .build();

    Letter savedLetter = letterRepository.save(newLetter);

    return new LetterResponse(savedLetter);
  }

  @Override
  @Transactional
  public ScheduleLetterResponse saveScheduleLetter(Long userId, ScheduleLetterRequestDto request) {

    if (!userRepository.existsByIdAndFamily_FamilyId(userId, request.getFamilyId())) {
      throw new IllegalArgumentException(
          "Sender ID " + userId + " is not a member of Family ID " + request.getFamilyId());
    }

    if (!userRepository.existsByIdAndFamily_FamilyId(
        request.getReceiverId(), request.getFamilyId())) {
      throw new IllegalArgumentException(
          "Receiver ID "
              + request.getReceiverId()
              + " is not a member of Family ID "
              + request.getFamilyId());
    }

    Letter newLetter =
        Letter.builder()
            .scheduleId(request.getScheduleId())
            .senderId(userId)
            .receiverId(request.getReceiverId())
            .familyId(request.getFamilyId())
            .letterType(LetterType.TEXT)
            .textContent(request.getTextContent())
            .isLiked(false)
            .build();

    Letter savedLetter = letterRepository.save(newLetter);

    return new ScheduleLetterResponse(savedLetter);
  }

  @Override
  @Transactional
  public List<LetterResponse> getSavedAllLetters(Long receiverId) {
    List<Letter> letters =
        letterRepository.findAllByReceiverIdAndIsSaved(receiverId, true); // isSaved = true

    return letters.stream()
        .sorted(
            Comparator.comparing(Letter::getCreatedAt)
                .thenComparing((l1, l2) -> l2.getId().compareTo(l1.getId())))
        .map(LetterResponse::new)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public List<LetterResponse> getNullSavedLetters(Long receiverId) {
    List<Letter> letters = letterRepository.findAllByReceiverIdAndIsSavedIsFalse(receiverId);
    return letters.stream().map(LetterResponse::new).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public LetterResponse updateLetterAsSaved(Long letterId) {
    Letter letter =
        letterRepository
            .findById(letterId)
            .orElseThrow(() -> new ResourceNotFoundException("Letter", letterId));

    letter.setSaved();
    letterRepository.save(letter);

    return new LetterResponse(letter);
  }

  @Override
  @Transactional
  public LetterResponse deleteLetter(Long letterId) {
    Letter letter =
        letterRepository
            .findById(letterId)
            .orElseThrow(() -> new ResourceNotFoundException("Letter", letterId));

    LetterResponse deletedLetterResponse = new LetterResponse(letter);
    letterRepository.delete(letter);

    return deletedLetterResponse;
  }

  @Override
  @Transactional
  public Map<String, String> getDownloadUrl(Long letterId) {
    Letter letter =
        letterRepository
            .findById(letterId)
            .orElseThrow(() -> new ResourceNotFoundException("Letter", letterId));

    String keyName = letter.getKeyName();
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(builder -> builder.bucket(bucketName).key(keyName).build())
            .build();

    String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();

    Map<String, String> response = new HashMap<>();
    response.put("presignedUrl", presignedUrl);
    response.put("keyName", keyName);
    return response;
  }

  @Override
  @Transactional
  public LetterResponse changeLikeState(Long letterId) {
    Letter letter =
        letterRepository
            .findById(letterId)
            .orElseThrow(() -> new ResourceNotFoundException("Letter", letterId));

    letter.changeIsLiked();

    return new LetterResponse(letterRepository.save(letter));
  }

  @Override
  @Transactional
  public LetterDatailResponse getLetterDatail(Long letterId) {
    Letter letter =
            letterRepository
                    .findById(letterId)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Letter with ID " + letterId + " not found"));

//    // 편지 읽음 상태 업데이트
//    if (!letter.isRead()) { // 이미 읽은 편지가 아니면 업데이트
//      letter.setIsRead(true);
//      letterRepository.save(letter);
//    }

    String senderNickname =
            userRepository
                    .findById(letter.getSenderId())
                    .map(User::getNickname)
                    .orElseThrow(
                            () ->
                                    new IllegalArgumentException(
                                            "Sender not found for ID: " + letter.getSenderId()));

    String receiverNickname =
            userRepository
                    .findById(letter.getReceiverId())
                    .map(User::getNickname)
                    .orElseThrow(
                            () ->
                                    new IllegalArgumentException(
                                            "Receiver not found for ID: " + letter.getReceiverId()));

    String scheduleName = null;
    if (letter.getScheduleId() != null && letter.getScheduleId() != 0) {
      scheduleName =
              scheduleRepository
                      .findById(letter.getScheduleId())
                      .map(Schedule::getTitle)
                      .orElseThrow(
                              () ->
                                      new IllegalArgumentException(
                                              "Schedule not found for ID: " + letter.getScheduleId()));
    }
    
    return LetterDatailResponse.builder()
            .letterId(letter.getId())
            .senderId(letter.getSenderId())
            .senderNickName(senderNickname)
            .receiverId(letter.getReceiverId())
            .receiverNickName(receiverNickname)
            .scheduleId(letter.getScheduleId())
            .scheduleName(scheduleName)
            .letterType(letter.getLetterType())
            .keyName(letter.getKeyName())
            .textContent(letter.getTextContent())
            .isLiked(letter.getIsLiked())
            .createdAt(letter.getCreatedAt())
            .build();
  }

    @Override
    @Transactional
    public List<LetterByDateResponse> getSavedLettersByMonth(Long receiverId, String yearMonth) {

        String[] yearMonthSplit = yearMonth.split("-");
        int year = Integer.parseInt(yearMonthSplit[0]);
        int month = Integer.parseInt(yearMonthSplit[1]);

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        List<Letter> letters = letterRepository.findAllByReceiverIdAndIsSavedAndCreatedAtBetween(
                receiverId, true,
                startOfMonth.atStartOfDay(),
                endOfMonth.atTime(LocalTime.MAX)
        );

        Map<LocalDate, List<LetterByMonthResponse>> groupedLetters = letters.stream()
                .map(letter -> new LetterByMonthResponse(
                        letter.getId(),
                        letter.getIsLiked(),
                        letter.getCreatedAt()
                ))
                .collect(Collectors.groupingBy(letter -> letter.getCreatedAt().toLocalDate()));

        return groupedLetters.entrySet().stream()
                .map(entry -> new LetterByDateResponse(
                        entry.getKey(),
                        entry.getValue(),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(LetterByDateResponse::getDate))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<LetterByDateDetailResponse> getSavedLettersByDate(Long receiverId, String yearMonth, int day) {
        String[] yearMonthSplit = yearMonth.split("-");
        int year = Integer.parseInt(yearMonthSplit[0]);
        int month = Integer.parseInt(yearMonthSplit[1]);

        LocalDate specificDate = LocalDate.of(year, month, day);

        List<Letter> letters = letterRepository.findAllByReceiverIdAndIsSavedAndCreatedAtBetween(
                receiverId, true,
                specificDate.atStartOfDay(),
                specificDate.atTime(LocalTime.MAX)
        );

        return letters.stream().map(letter -> {
            String senderNickname = userRepository.findById(letter.getSenderId())
                    .map(User::getNickname)
                    .orElse("Unknown Sender");

            return LetterByDateDetailResponse.builder()
                    .letterId(letter.getId())
                    .letterType(letter.getLetterType())
                    .senderNickname(senderNickname)
                    .textContent(letter.getTextContent())
                    .isLiked(letter.getIsLiked())
                    .createdAt(letter.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<LettersByLikeDateResponse> getLikedLettersByMonth(Long receiverId, String yearMonth) {
        String[] yearMonthSplit = yearMonth.split("-");
        int year = Integer.parseInt(yearMonthSplit[0]);
        int month = Integer.parseInt(yearMonthSplit[1]);

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        List<Letter> likedLetters = letterRepository.findAllByReceiverIdAndIsLikedAndCreatedAtBetween(
                receiverId, true,
                startOfMonth.atStartOfDay(),
                endOfMonth.atTime(LocalTime.MAX)
        );

        Map<LocalDate, List<LetterByLikeResponse>> groupedLetters = likedLetters.stream()
                .map(letter -> {
                    String senderNickName = userRepository.findById(letter.getSenderId())
                            .map(User::getNickname)
                            .orElse("알 수 없는 사용자");
                    return LetterByLikeResponse.builder()
                            .letterId(letter.getId())
                            .senderId(letter.getSenderId())
                            .senderNickName(senderNickName)
                            .isLiked(letter.getIsLiked())
                            .createdAt(letter.getCreatedAt())
                            .build();
                })
                .collect(Collectors.groupingBy(letter -> letter.getCreatedAt().toLocalDate()));

        return groupedLetters.entrySet().stream()
                .map(entry -> new LettersByLikeDateResponse(
                        entry.getKey(),
                        entry.getValue(),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(LettersByLikeDateResponse::getDate))
                .collect(Collectors.toList());
    }
    
}
