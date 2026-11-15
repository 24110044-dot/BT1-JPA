package vn.iotstar.service;

import vn.iotstar.entity.Cart;

public interface ICartService {

    Cart getCart(Integer userId);

    int itemCount(Integer userId);

    Cart add(Integer userId, Integer productId, int quantity);

    Cart update(Integer userId, Integer cartItemId, int quantity);

    void remove(Integer userId, Integer cartItemId);

    void clear(Integer userId);
}
