package io.teammistake.chatjamo.controllers

import io.teammistake.chatjamo.service.LeaderboardService
import io.teammistake.chatjamo.service.getUser
import io.teammistake.chatjamo.service.getUserId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/leaderboard")
class LeaderboardController {

    @Autowired
    lateinit var leaderboardService: LeaderboardService;


    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    suspend fun getMe(): LeaderboardService.Position {
        return leaderboardService.getPosition(getUser()?.getUserId() ?: "");
    }

    @GetMapping
    suspend fun getLeaderboard(@RequestParam("from", defaultValue = "0") from: Int, @RequestParam("count", defaultValue = "10") cnt: Int): List<LeaderboardService.Position> {
        return leaderboardService.getLeaderboard(from, cnt)
    }
}