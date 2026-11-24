package br.com.acasadotenis.pedidos_service.repository;

import br.com.acasadotenis.pedidos_service.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByCode(String code);

    /** Pedidos de uma conta de cliente: os do e-mail dela e os que ela adicionou pelo código. */
    @Query("select distinct o from Order o left join o.followers f "
            + "where lower(o.customerEmail) = :email or lower(f) = :email")
    List<Order> findForAccount(@Param("email") String email);
}
