package com.skillswap.skillswap.dto;

import com.skillswap.skillswap.entity.SwapRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SwapResponse {

    private Long id;

    private Long senderId;
    private String senderName;

    private Long receiverId;
    private String receiverName;

    private Long offeredSkillId;
    private String offeredSkillName;

    private Long requestedSkillId;
    private String requestedSkillName;

    private SwapRequest.Status status;

    private LocalDateTime createdAt;

    public static SwapResponse fromEntity(SwapRequest swap) {

        return new SwapResponse(
                swap.getId(),

                swap.getSender().getId(),
                swap.getSender().getName(),

                swap.getReceiver().getId(),
                swap.getReceiver().getName(),

                swap.getOfferedSkill().getId(),
                swap.getOfferedSkill().getName(),

                swap.getRequestedSkill().getId(),
                swap.getRequestedSkill().getName(),

                swap.getStatus(),
                swap.getCreatedAt()
        );
    }
}