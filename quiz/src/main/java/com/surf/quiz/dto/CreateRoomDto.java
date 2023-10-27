package com.surf.quiz.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateRoomDto {
    String roomName;
    int quizCnt;
    boolean single;
    List<Integer> pages;
    List<Integer> sharePages;
    private List<UserDto> inviteUsers;
}
