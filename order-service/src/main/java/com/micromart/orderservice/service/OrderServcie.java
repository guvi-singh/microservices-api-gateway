package com.micromart.orderservice.service;

import com.micromart.orderservice.dto.InventoryResponse;
import com.micromart.orderservice.dto.OrderLineItemsDto;
import com.micromart.orderservice.dto.OrderRequest;
import com.micromart.orderservice.model.Order;
import com.micromart.orderservice.model.OrderLineItems;
import com.micromart.orderservice.respository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional
public class OrderServcie {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        //call inventory and place order if item is in stock
        InventoryResponse[] inventoryResponseArray=webClientBuilder.build().get().uri("http://inventory-service/api/inventory", uriBuilder ->
                        uriBuilder.queryParam("skuCode" , skuCodes).build()).retrieve()
                        .bodyToMono(InventoryResponse[].class).block();
       boolean allProductsInStock= Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse :: isInStock);
        if(allProductsInStock)
        {
            orderRepository.save(order);

        }else {
            throw new IllegalArgumentException("Product is not in stock, please try again");
        }
    }
    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
