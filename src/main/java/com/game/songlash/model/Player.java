package com.game.songlash.model;

import java.util.Optional;

public record Player(String sessionId, Optional<String> name, Optional<String> roomId) {}