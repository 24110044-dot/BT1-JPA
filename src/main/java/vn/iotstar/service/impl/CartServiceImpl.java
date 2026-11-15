package vn.iotstar.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.entity.Cart;
import vn.iotstar.entity.CartItem;
import vn.iotstar.entity.Product;
import vn.iotstar.entity.User;
import vn.iotstar.exception.InsufficientStockException;
import vn.iotstar.exception.UserNotFoundException;
import vn.iotstar.repository.CartItemRepository;
import vn.iotstar.repository.CartRepository;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.ICartService;

@Service
@Transactional
public class CartServiceImpl implements ICartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
            ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Cart getCart(Integer userId) {
        Cart cart = cartRepository.findWithItemsByUser_Id(userId).orElseGet(() -> new Cart());
        if (cart.getCartId() != null) {
            cart.getItems().forEach(item -> item.setUnitPrice(item.getProduct().getPrice()));
        }
        return cart;
    }

    @Override
    @Transactional(readOnly = true)
    public int itemCount(Integer userId) {
        return cartRepository.findWithItemsByUser_Id(userId).map(Cart::getTotalQuantity).orElse(0);
    }

    @Override
    public Cart add(Integer userId, Integer productId, int quantity) {
        requirePositive(quantity);
        Cart cart = getOrCreateLockedCart(userId);
        Product product = productRepository.findWithCategoryByProductId(productId)
                .filter(p -> Boolean.TRUE.equals(p.getStatus()))
                .orElseThrow(() -> new InsufficientStockException("Sản phẩm hiện không kinh doanh."));
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(productId)).findFirst().orElse(null);
        int nextQuantity = quantity + (existing == null ? 0 : existing.getQuantity());
        ensureStock(product, nextQuantity);
        if (existing == null) {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            cart.addItem(item);
        } else {
            existing.setQuantity(nextQuantity);
            existing.setUnitPrice(product.getPrice());
        }
        return cartRepository.save(cart);
    }

    @Override
    public Cart update(Integer userId, Integer cartItemId, int quantity) {
        requirePositive(quantity);
        Cart cart = getOrCreateLockedCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(candidate -> candidate.getCartItemId().equals(cartItemId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Sản phẩm không thuộc giỏ hàng của bạn."));
        Product product = productRepository.findById(item.getProduct().getProductId())
                .orElseThrow(() -> new InsufficientStockException("Sản phẩm không còn tồn tại."));
        if (!Boolean.TRUE.equals(product.getStatus())) {
            throw new InsufficientStockException("Sản phẩm đã ngừng kinh doanh.");
        }
        ensureStock(product, quantity);
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        return cartRepository.save(cart);
    }

    @Override
    public void remove(Integer userId, Integer cartItemId) {
        Cart cart = getOrCreateLockedCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(candidate -> candidate.getCartItemId().equals(cartItemId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Sản phẩm không thuộc giỏ hàng của bạn."));
        cart.removeItem(item);
        cartRepository.save(cart);
    }

    @Override
    public void clear(Integer userId) {
        Cart cart = getOrCreateLockedCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateLockedCart(Integer userId) {
        Optional<Cart> existing = cartRepository.findLockedByUser_Id(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.saveAndFlush(cart);
    }

    private void ensureStock(Product product, int quantity) {
        if (quantity > product.getQuantity()) {
            throw new InsufficientStockException("Số lượng vượt quá tồn kho hiện tại (" + product.getQuantity() + ").");
        }
    }

    private void requirePositive(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        }
    }
}
