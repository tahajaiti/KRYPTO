package com.krypto.coin.service;

public interface MarketSimulationService {

    void runSimulationTick();

    void runScheduledTick();
}
