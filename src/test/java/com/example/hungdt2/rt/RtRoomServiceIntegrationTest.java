package com.example.hungdt2.rt;

import com.example.hungdt2.rt.dto.CreateRtRoomRequest;
import com.example.hungdt2.rt.entity.RtRoomEntity;
import com.example.hungdt2.user.entity.UserEntity;
import com.example.hungdt2.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional
public class RtRoomServiceIntegrationTest {

    @Autowired
    private RtRoomService rtRoomService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void publicRoomShouldAppearInListingEvenIfNotActive() {
        UserEntity u = new UserEntity();
        u.setUsername("testuser-rt");
        u.setEmail("testuser-rt@example.com");
        u.setPhone("+10000000000");
        u.setPasswordHash("x");
        userRepository.save(u);

        CreateRtRoomRequest req = new CreateRtRoomRequest();
        req.name = "Public Room Test";
        req.isPublic = true;
        req.description = "desc";

        var created = rtRoomService.createRoom(u.getId(), req);
        Assertions.assertNotNull(created);
        Assertions.assertNotNull(created.id);

        List<RtRoomEntity> pubs = rtRoomService.listPublicRooms();
        boolean found = pubs.stream().anyMatch(r -> r.getId().equals(created.id));
        Assertions.assertTrue(found, "Newly created public room should appear in public listing");
    }
}