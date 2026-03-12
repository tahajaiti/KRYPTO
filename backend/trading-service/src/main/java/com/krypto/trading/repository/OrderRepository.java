package com.krypto.trading.repository;

import com.krypto.trading.entity.Order;
import com.krypto.trading.entity.OrderSide;
import com.krypto.trading.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Order> findByCoinIdAndSideAndStatusInOrderByPriceAscCreatedAtAsc(
            UUID coinId,
            OrderSide side,
            Collection<OrderStatus> statuses
    );

    List<Order> findByCoinIdAndSideAndStatusInOrderByPriceDescCreatedAtAsc(
            UUID coinId,
            OrderSide side,
            Collection<OrderStatus> statuses
    );

    List<Order> findByCoinIdAndSideAndStatusInAndPriceLessThanEqualOrderByPriceAscCreatedAtAsc(
            UUID coinId,
            OrderSide side,
            Collection<OrderStatus> statuses,
            BigDecimal price
    );

    List<Order> findByCoinIdAndSideAndStatusInAndPriceGreaterThanEqualOrderByPriceDescCreatedAtAsc(
            UUID coinId,
            OrderSide side,
            Collection<OrderStatus> statuses,
            BigDecimal price
    );
}
