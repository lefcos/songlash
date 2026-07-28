package com.game.songlash;

import java.util.Optional;

public record Player(String sessionId, String name, Optional<String> roomId) {}