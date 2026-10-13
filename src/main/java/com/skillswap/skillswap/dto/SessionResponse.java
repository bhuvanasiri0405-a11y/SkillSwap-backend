package com.skillswap.skillswap.dto;

import com.skillswap.skillswap.entity.Session;
import java.time.LocalDate;
import java.time.LocalTime;

public record SessionResponse(Long id, Long swapRequestId, Long senderId, String senderName,
        Long receiverId, String receiverName, LocalDate sessionDate, LocalTime sessionTime,
        Session.Status status, String googleMeetUrl) {
    public static SessionResponse fromEntity(Session session) {
        var swap = session.getSwapRequest();
        return new SessionResponse(session.getId(), swap.getId(), swap.getSender().getId(),
                swap.getSender().getName(), swap.getReceiver().getId(), swap.getReceiver().getName(),
                session.getSessionDate(), session.getSessionTime(), session.getStatus(), session.getGoogleMeetUrl());
    }
}
