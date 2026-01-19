package com.example.hungdt2.user;

import com.example.hungdt2.user.dto.CreateFriendRequest;
import com.example.hungdt2.user.dto.FriendRequestItem;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.repository.FriendRequestRepository;
import com.example.hungdt2.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.http.ResponseEntity;

@SpringBootTest
public class FriendControllerIntegrationTest {

    @Autowired
    FriendController friendController;

    @Autowired
    UserRepository userRepository;

    @Autowired
    FriendRequestRepository friendRequestRepository;

    private UserEntity alice;
    private UserEntity bob;

    @BeforeEach
    public void setup() {
        friendRequestRepository.deleteAll();
        userRepository.deleteAll();

        long suffix = System.nanoTime();

        alice = new UserEntity();
        alice.setUsername("alice" + suffix);
        alice.setEmail("alice" + suffix + "@example.com");
        alice.setPhone("+100000" + (suffix % 1000000));
        alice.setPasswordHash("x");
        alice.setDisplayName("Alice A");
        userRepository.save(alice);

        bob = new UserEntity();
        bob.setUsername("bob" + suffix);
        bob.setEmail("bob" + suffix + "@example.com");
        bob.setPhone("+100001" + (suffix % 1000000));
        bob.setPasswordHash("x");
        bob.setDisplayName("Bob B");
        userRepository.save(bob);
    }

    @Test
    public void testSendRequestIncludesNames() {
        CreateFriendRequest req = new CreateFriendRequest(bob.getPhone());
        TestingAuthenticationToken auth = new TestingAuthenticationToken(alice.getId(), null);
        ResponseEntity<?> resp = friendController.sendRequest(auth, req);
        Object body = resp.getBody();
        // ApiResponse<FriendRequestItem>
        var api = (com.example.hungdt2.common.ApiResponse<FriendRequestItem>) body;
        var data = api.data();
        Assertions.assertNotNull(data);
        Assertions.assertEquals("Alice A", data.requesterName());
        Assertions.assertEquals("Bob B", data.recipientName());
    }
}
