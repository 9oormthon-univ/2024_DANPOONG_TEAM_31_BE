package com.danpoong.withu.letter.service;

import com.danpoong.withu.letter.controller.response.*;
import java.util.List;
import java.util.Map;

import com.danpoong.withu.letter.controller.response.LetterDatailResponse;
import com.danpoong.withu.letter.controller.response.LetterResponse;
import com.danpoong.withu.letter.controller.response.ScheduleLetterResponse;
import com.danpoong.withu.letter.dto.LetterReqDto;
import com.danpoong.withu.letter.dto.ScheduleLetterRequestDto;
import com.danpoong.withu.letter.dto.TextLetterRequestDto;

public interface LetterService {

    Map<String, String> generatePresignedUrl(Long familyId, Long senderId, Long receiverId);
    LetterResponse saveLetter(Long userId, LetterReqDto request);
    LetterResponse saveTextLetter(Long userId, TextLetterRequestDto request);
    ScheduleLetterResponse saveScheduleLetter(Long userId, ScheduleLetterRequestDto request);
    List<LetterResponse> getSavedAllLetters(Long receiverId);
    List<LetterResponse> getNullSavedLetters(Long receiverId);
    Map<String, String> getDownloadUrl(Long letterId);
    LetterResponse deleteLetter(Long letterId);
    LetterResponse updateLetterAsSaved(Long letterId);
    LetterResponse changeLikeState(Long letterId);
    LetterDatailResponse getLetterDatail(Long letterId);
    List<LetterByDateResponse> getSavedLettersByMonth(Long receiverId, String yearMonth);
    List<LetterByDateDetailResponse> getSavedLettersByDate(Long receiverId, String yearMonth, int day);
    List<LettersByLikeDateResponse> getLikedLettersByMonth(Long receiverId, String yearMonth);

}
