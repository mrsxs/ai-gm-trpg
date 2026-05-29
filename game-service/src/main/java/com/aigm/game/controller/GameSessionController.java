package com.aigm.game.controller;

import com.aigm.common.constant.RoleConst;
import com.aigm.common.result.R;
import com.aigm.common.util.PageQuery;
import com.aigm.common.util.PageResult;
import com.aigm.common.web.UserContext;
import com.aigm.game.dto.*;
import com.aigm.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 对局/回合（基线 §5.3，PLAYER 本人）。身份取自网关 X-User-* 头。 */
@RestController
@RequestMapping("/api/game/sessions")
@RequiredArgsConstructor
public class GameSessionController {

    private final GameService gameService;

    @PostMapping
    public R<StartSessionVO> start(@Valid @RequestBody StartSessionDTO dto) {
        Long uid = requirePlayer();
        return R.ok(gameService.startSession(uid, dto.getScenarioId()));
    }

    @PostMapping("/{id}/turns")
    public R<TurnResultVO> submitTurn(@PathVariable Long id, @Valid @RequestBody SubmitTurnDTO dto) {
        Long uid = requirePlayer();
        return R.ok(gameService.submitTurn(uid, id, dto.getPlayerInput()));
    }

    @GetMapping("/{id}")
    public R<SessionDetailVO> detail(@PathVariable Long id) {
        Long uid = requirePlayer();
        return R.ok(gameService.getSessionDetail(uid, id));
    }

    @GetMapping
    public R<PageResult<SessionVO>> list(@RequestParam(required = false) Long page,
                                         @RequestParam(required = false) Long size,
                                         @RequestParam(required = false) Integer status) {
        Long uid = requirePlayer();
        return R.ok(gameService.pageSessions(uid, PageQuery.of(page, size), status));
    }

    @PutMapping("/{id}/abandon")
    public R<Boolean> abandon(@PathVariable Long id) {
        Long uid = requirePlayer();
        gameService.abandon(uid, id);
        return R.ok(true);
    }

    @GetMapping("/{id}/state")
    public R<GameStateVO> state(@PathVariable Long id) {
        Long uid = requirePlayer();
        return R.ok(gameService.getState(uid, id));
    }

    private Long requirePlayer() {
        UserContext.requireAnyRole(RoleConst.PLAYER, RoleConst.ADMIN);
        return UserContext.requireUserId();
    }
}
