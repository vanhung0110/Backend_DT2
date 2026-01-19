package com.example.hungdt2.user;

import com.example.hungdt2.user.entity.FriendRequestEntity;
import com.example.hungdt2.user.entity.FriendshipEntity;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.repository.FriendRequestRepository;
import com.example.hungdt2.user.repository.FriendshipRepository;
import com.example.hungdt2.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class FriendServiceIntegrationTest {

    @Autowired
    FriendService friendService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    FriendRequestRepository friendRequestRepository;

    @Autowired
    FriendshipRepository friendshipRepository;

    private UserEntity alice;
    private UserEntity bob;

    @BeforeEach
    public void setup() {
        friendRequestRepository.deleteAll();
        friendshipRepository.deleteAll();
        userRepository.deleteAll();

        long suffix = System.nanoTime();

        alice = new UserEntity();
        alice.setUsername("alice" + suffix);
        alice.setEmail("alice" + suffix + "@example.com");
        alice.setPhone("+100000" + (suffix % 1000000));
        alice.setPasswordHash("x");
        userRepository.save(alice);

        bob = new UserEntity();
        bob.setUsername("bob" + suffix);
        bob.setEmail("bob" + suffix + "@example.com");
        bob.setPhone("+100001" + (suffix % 1000000));
        bob.setPasswordHash("x");
        userRepository.save(bob);
    }

    @Test
    @Transactional
    public void testSendAndAccept() {
        FriendRequestEntity req = friendService.sendRequest(alice.getId(), bob.getPhone());
        Assertions.assertEquals("PENDING", req.getStatus());

        // incoming for bob
        var incoming = friendService.listIncomingRequests(bob.getId(), 0, 10);
        Assertions.assertEquals(1, incoming.getTotalElements());

        // accept
        friendService.acceptRequest(bob.getId(), alice.getId());
        var updated = friendRequestRepository.findByRequesterIdAndRecipientId(alice.getId(), bob.getId()).get();
        Assertions.assertEquals("APPROVED", updated.getStatus());

        // friendships created both ways
        Assertions.assertTrue(friendshipRepository.findByUserIdAndFriendId(alice.getId(), bob.getId()).isPresent());
        Assertions.assertTrue(friendshipRepository.findByUserIdAndFriendId(bob.getId(), alice.getId()).isPresent());
    }

    @Test
    @Transactional
    public void testReversePendingAutoApprove() {
        // Bob sends to Alice first
        FriendRequestEntity r = new FriendRequestEntity();
        r.setRequesterId(bob.getId());
        r.setRecipientId(alice.getId());
        r.setStatus("PENDING");
        friendRequestRepository.save(r);

        // Now Alice sends to Bob -> should auto-approve
        FriendRequestEntity out = friendService.sendRequest(alice.getId(), bob.getPhone());
        // the reverse request should be APPROVED
        var reverse = friendRequestRepository.findByRequesterIdAndRecipientId(bob.getId(), alice.getId()).get();
        Assertions.assertEquals("APPROVED", reverse.getStatus());
        // friendships exist
        Assertions.assertTrue(friendshipRepository.findByUserIdAndFriendId(alice.getId(), bob.getId()).isPresent());
        Assertions.assertTrue(friendshipRepository.findByUserIdAndFriendId(bob.getId(), alice.getId()).isPresent());
    }

    @Test
    @Transactional
    public void testReviveRejected() {
        FriendRequestEntity r = new FriendRequestEntity();
        r.setRequesterId(alice.getId());
        r.setRecipientId(bob.getId());
        r.setStatus("REJECTED");
        friendRequestRepository.save(r);

        FriendRequestEntity out = friendService.sendRequest(alice.getId(), bob.getPhone());
        Assertions.assertEquals("PENDING", out.getStatus());
    }
}
