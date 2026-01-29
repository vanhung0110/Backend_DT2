package com.example.hungdt2.user;

import com.example.hungdt2.exceptions.BadRequestException;
import com.example.hungdt2.exceptions.ConflictException;
import com.example.hungdt2.exceptions.NotFoundException;
import com.example.hungdt2.user.entity.FriendRequestEntity;
import com.example.hungdt2.user.entity.FriendshipEntity;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.repository.FriendRequestRepository;
import com.example.hungdt2.user.repository.FriendshipRepository;
import com.example.hungdt2.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final com.example.hungdt2.websocket.UserWsHandler userWsHandler;

    public FriendService(FriendRequestRepository friendRequestRepository, FriendshipRepository friendshipRepository, UserRepository userRepository, com.example.hungdt2.websocket.UserWsHandler userWsHandler) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.userWsHandler = userWsHandler;
    }

    @Transactional
    public FriendRequestEntity sendRequest(Long requesterId, String phone) {
        UserEntity recipient = userRepository.findByPhone(phone).orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found by phone"));
        if (recipient.getId().equals(requesterId)) throw new BadRequestException("CANNOT_ADD_SELF", "Cannot add yourself as a friend");

        // check already friends
        if (friendshipRepository.findByUserIdAndFriendId(requesterId, recipient.getId()).isPresent()) {
            throw new ConflictException("ALREADY_FRIEND", "Already friends");
        }

        // check reverse pending: recipient -> requester
        var reverseOpt = friendRequestRepository.findByRequesterIdAndRecipientId(recipient.getId(), requesterId);
        if (reverseOpt.isPresent() && "PENDING".equals(reverseOpt.get().getStatus())) {
            // auto-approve reverse and create friendships
            FriendRequestEntity reverse = reverseOpt.get();
            reverse.setStatus("APPROVED");
            friendRequestRepository.save(reverse);

            createFriendshipRows(requesterId, recipient.getId());
            return reverse;
        }

        // check existing forward request
        var existingOpt = friendRequestRepository.findByRequesterIdAndRecipientId(requesterId, recipient.getId());
        if (existingOpt.isPresent()) {
            var ex = existingOpt.get();
            if ("PENDING".equals(ex.getStatus())) throw new ConflictException("REQUEST_ALREADY_PENDING", "Friend request already pending");
            if ("REJECTED".equals(ex.getStatus()) || "CANCELLED".equals(ex.getStatus())) {
                ex.setStatus("PENDING");
                return friendRequestRepository.save(ex);
            }
            if ("APPROVED".equals(ex.getStatus())) throw new ConflictException("ALREADY_FRIEND", "Already friends");
        }

        // create new request
        FriendRequestEntity req = new FriendRequestEntity();
        req.setRequesterId(requesterId);
        req.setRecipientId(recipient.getId());
        req.setStatus("PENDING");
        try {
            FriendRequestEntity saved = friendRequestRepository.save(req);
            // notify recipient if online (include display names)
            var requester = userRepository.findById(saved.getRequesterId()).orElse(null);
            var requesterName = requester == null ? null : (requester.getDisplayName() != null ? requester.getDisplayName() : requester.getUsername());
            var recipientName = recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getUsername();
            userWsHandler.sendToUser(recipient.getId(), java.util.Map.of("type", "friend.request.created", "data", java.util.Map.of("id", saved.getId(), "requesterId", saved.getRequesterId(), "recipientId", saved.getRecipientId(), "requesterName", requesterName, "recipientName", recipientName, "status", saved.getStatus())));
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // unique constraint violated -> try to return existing
            var existing = friendRequestRepository.findByRequesterIdAndRecipientId(requesterId, recipient.getId()).orElseThrow(() -> new ConflictException("REQUEST_FAILED", "Failed to create request"));
            // still notify recipient in case the existing was REJECTED revived earlier
            userWsHandler.sendToUser(recipient.getId(), java.util.Map.of("type", "friend.request.created", "data", java.util.Map.of("id", existing.getId(), "requesterId", existing.getRequesterId(), "recipientId", existing.getRecipientId(), "status", existing.getStatus())));
            return existing;
        }
    }

    @Transactional
    public void acceptRequest(Long recipientId, Long requesterId) {
        FriendRequestEntity req = friendRequestRepository.findByRequesterIdAndRecipientId(requesterId, recipientId).orElseThrow(() -> new NotFoundException("REQUEST_NOT_FOUND", "Request not found"));
        if (!"PENDING".equals(req.getStatus())) throw new ConflictException("REQUEST_NOT_PENDING", "Request not pending");
        req.setStatus("APPROVED");
        friendRequestRepository.save(req);
        createFriendshipRows(requesterId, recipientId);
        // notify requester that request was approved (include names)
        var requester = userRepository.findById(req.getRequesterId()).orElse(null);
        var recipient = userRepository.findById(req.getRecipientId()).orElse(null);
        var requesterName = requester == null ? null : (requester.getDisplayName() != null ? requester.getDisplayName() : requester.getUsername());
        var recipientName = recipient == null ? null : (recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getUsername());
        userWsHandler.sendToUser(requesterId, java.util.Map.of("type", "friend.request.updated", "data", java.util.Map.of("id", req.getId(), "requesterId", req.getRequesterId(), "recipientId", req.getRecipientId(), "requesterName", requesterName, "recipientName", recipientName, "status", req.getStatus())));
    }

    @Transactional
    public void rejectRequest(Long recipientId, Long requesterId) {
        FriendRequestEntity req = friendRequestRepository.findByRequesterIdAndRecipientId(requesterId, recipientId).orElseThrow(() -> new NotFoundException("REQUEST_NOT_FOUND", "Request not found"));
        if (!"PENDING".equals(req.getStatus())) throw new ConflictException("REQUEST_NOT_PENDING", "Request not pending");
        req.setStatus("REJECTED");
        friendRequestRepository.save(req);
        // notify requester that request was rejected (include names)
        var requester = userRepository.findById(req.getRequesterId()).orElse(null);
        var recipient = userRepository.findById(req.getRecipientId()).orElse(null);
        var requesterName = requester == null ? null : (requester.getDisplayName() != null ? requester.getDisplayName() : requester.getUsername());
        var recipientName = recipient == null ? null : (recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getUsername());
        userWsHandler.sendToUser(req.getRequesterId(), java.util.Map.of("type", "friend.request.updated", "data", java.util.Map.of("id", req.getId(), "requesterId", req.getRequesterId(), "recipientId", req.getRecipientId(), "requesterName", requesterName, "recipientName", recipientName, "status", req.getStatus())));
    }

    @Transactional
    public void cancelRequest(Long requesterId, Long recipientId) {
        FriendRequestEntity req = friendRequestRepository.findByRequesterIdAndRecipientId(requesterId, recipientId).orElseThrow(() -> new NotFoundException("REQUEST_NOT_FOUND", "Request not found"));
        if (!requesterId.equals(req.getRequesterId())) throw new ConflictException("NOT_REQUESTER", "Only requester can cancel");
        if (!"PENDING".equals(req.getStatus())) throw new ConflictException("REQUEST_NOT_PENDING", "Request not pending");
        req.setStatus("CANCELLED");
        friendRequestRepository.save(req);
        // notify recipient that request was cancelled (include names)
        var requester = userRepository.findById(req.getRequesterId()).orElse(null);
        var recipient = userRepository.findById(req.getRecipientId()).orElse(null);
        var requesterName = requester == null ? null : (requester.getDisplayName() != null ? requester.getDisplayName() : requester.getUsername());
        var recipientName = recipient == null ? null : (recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getUsername());
        userWsHandler.sendToUser(req.getRecipientId(), java.util.Map.of("type", "friend.request.updated", "data", java.util.Map.of("id", req.getId(), "requesterId", req.getRequesterId(), "recipientId", req.getRecipientId(), "requesterName", requesterName, "recipientName", recipientName, "status", req.getStatus())));
    }

    private void createFriendshipRows(Long userA, Long userB) {
        try {
            if (friendshipRepository.findByUserIdAndFriendId(userA, userB).isEmpty()) {
                FriendshipEntity f = new FriendshipEntity();
                f.setUserId(userA);
                f.setFriendId(userB);
                friendshipRepository.save(f);
            }
            if (friendshipRepository.findByUserIdAndFriendId(userB, userA).isEmpty()) {
                FriendshipEntity f2 = new FriendshipEntity();
                f2.setUserId(userB);
                f2.setFriendId(userA);
                friendshipRepository.save(f2);
            }
        } catch (DataIntegrityViolationException ex) {
            // ignore duplicates from concurrent creates
        }
    }

    @Transactional(readOnly = true)
    public Page<FriendRequestEntity> listIncomingRequests(Long recipientId, int page, int size) {
        return friendRequestRepository.findByRecipientIdAndStatus(recipientId, "PENDING", PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<FriendRequestEntity> listOutgoingRequests(Long requesterId, int page, int size) {
        return friendRequestRepository.findByRequesterIdAndStatus(requesterId, "PENDING", PageRequest.of(page, size));
    }

    @Transactional
    public void unfriend(Long userId, Long friendId) {
        // remove both sides if present
        friendshipRepository.deleteByUserIdAndFriendId(userId, friendId);
        friendshipRepository.deleteByUserIdAndFriendId(friendId, userId);
        // emit ws events to update clients
        try {
            userWsHandler.sendToUser(userId, java.util.Map.of("type", "friend.removed", "data", java.util.Map.of("friendId", friendId)));
            userWsHandler.sendToUser(friendId, java.util.Map.of("type", "friend.removed", "data", java.util.Map.of("friendId", userId)));
        } catch (Exception e) {
            // ignore ws errors
        }
    }
}
