package com.example.aikef.service;

import com.example.aikef.dto.CustomerDto;
import com.example.aikef.dto.CustomerTokenResponse;
import com.example.aikef.dto.request.CreateCustomerRequest;
import com.example.aikef.dto.request.UpdateCustomerRequest;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.Channel;
import com.example.aikef.model.ChatSession;
import com.example.aikef.model.Customer;
import com.example.aikef.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final EntityMapper entityMapper;
    private final CustomerTokenService customerTokenService;
    private final ChatSessionService chatSessionService;

    public CustomerService(CustomerRepository customerRepository,
                          EntityMapper entityMapper,
                          CustomerTokenService customerTokenService,
                          ChatSessionService chatSessionService) {
        this.customerRepository = customerRepository;
        this.entityMapper = entityMapper;
        this.customerTokenService = customerTokenService;
        this.chatSessionService = chatSessionService;
    }

    public Page<CustomerDto> listCustomers(String name, Channel channel, String tag, Boolean active, Pageable pageable) {
        Specification<Customer> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (name != null && !name.isBlank()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), 
                    "%" + name.toLowerCase() + "%"
                ));
            }
            
            if (channel != null) {
                predicates.add(criteriaBuilder.equal(root.get("primaryChannel"), channel));
            }
            
            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }
            
            // 标签查询（JSON 数组包含）
            if (tag != null && !tag.isBlank()) {
                predicates.add(criteriaBuilder.like(
                    root.get("tags").as(String.class),
                    "%\"" + tag + "\"%"
                ));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return customerRepository.findAll(spec, pageable)
                .map(entityMapper::toCustomerDto);
    }

    public CustomerDto getCustomer(UUID id) {
        Customer customer = findById(id);
        return entityMapper.toCustomerDto(customer);
    }

    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        // 校验邮箱唯一性
        if (request.email() != null && customerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("该邮箱已被使用");
        }
        
        // 校验手机号唯一性
        if (request.phone() != null && customerRepository.existsByPhone(request.phone())) {
            throw new IllegalArgumentException("该手机号已被使用");
        }
        
        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setPrimaryChannel(request.primaryChannel());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setWechatOpenId(request.wechatOpenId());
        customer.setWhatsappId(request.whatsappId());
        customer.setLineId(request.lineId());
        customer.setTelegramId(request.telegramId());
        customer.setFacebookId(request.facebookId());
        customer.setAvatarUrl(request.avatarUrl());
        customer.setLocation(request.location());
        customer.setNotes(request.notes());
        
        if (request.customFields() != null) {
            customer.setCustomFields(request.customFields());
        }
        
        Customer saved = customerRepository.save(customer);
        return entityMapper.toCustomerDto(saved);
    }

    @Transactional
    public CustomerDto updateCustomer(UUID id, UpdateCustomerRequest request) {
        Customer customer = findById(id);
        
        if (request.name() != null) {
            customer.setName(request.name());
        }
        
        if (request.primaryChannel() != null) {
            customer.setPrimaryChannel(request.primaryChannel());
        }
        
        if (request.email() != null && !request.email().equals(customer.getEmail())) {
            if (customerRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("该邮箱已被使用");
            }
            customer.setEmail(request.email());
        }
        
        if (request.phone() != null && !request.phone().equals(customer.getPhone())) {
            if (customerRepository.existsByPhone(request.phone())) {
                throw new IllegalArgumentException("该手机号已被使用");
            }
            customer.setPhone(request.phone());
        }
        
        if (request.wechatOpenId() != null) {
            customer.setWechatOpenId(request.wechatOpenId());
        }
        
        if (request.whatsappId() != null) {
            customer.setWhatsappId(request.whatsappId());
        }
        
        if (request.lineId() != null) {
            customer.setLineId(request.lineId());
        }
        
        if (request.telegramId() != null) {
            customer.setTelegramId(request.telegramId());
        }
        
        if (request.facebookId() != null) {
            customer.setFacebookId(request.facebookId());
        }
        
        if (request.avatarUrl() != null) {
            customer.setAvatarUrl(request.avatarUrl());
        }
        
        if (request.location() != null) {
            customer.setLocation(request.location());
        }
        
        if (request.notes() != null) {
            customer.setNotes(request.notes());
        }
        
        if (request.customFields() != null) {
            customer.setCustomFields(request.customFields());
        }
        
        if (request.active() != null) {
            customer.setActive(request.active());
        }
        
        Customer updated = customerRepository.save(customer);
        return entityMapper.toCustomerDto(updated);
    }

    @Transactional
    public void deleteCustomer(UUID id) {
        Customer customer = findById(id);
        customerRepository.delete(customer);
    }

    @Transactional
    public void updateLastInteraction(UUID customerId) {
        Customer customer = findById(customerId);
        customer.setLastInteractionAt(Instant.now());
        customerRepository.save(customer);
    }

    public Customer findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("客户不存在"));
    }

    /** 为客户生成访客 Token（用于 WebSocket 连接），并创建聊天会话（无元数据） */
    @Transactional
    public CustomerTokenResponse generateCustomerToken(UUID customerId) {
        return generateCustomerToken(customerId, null);
    }

    /** 
     * 为客户生成访客 Token（用于 WebSocket 连接），并创建聊天会话（带元数据）
     * 
     * @param customerId 客户ID
     * @param metadata 会话元数据，可包含：
     *                 - categoryId: 会话分类ID (String，UUID格式)
     *                 - source: 来源渠道
     *                 - referrer: 来源页面
     *                 - device: 设备信息
     *                 - 其他自定义字段...
     */
    @Transactional
    public CustomerTokenResponse generateCustomerToken(UUID customerId, Map<String, Object> metadata) {
        Customer customer = findById(customerId);
        
        // 创建聊天会话并分配客服（支持元数据）
        ChatSession session = chatSessionService.createSessionForCustomer(customer, metadata);
        
        // 生成 Token
        String token = customerTokenService.issueToken(customer);
        
        return new CustomerTokenResponse(
                customer.getId(),
                token,
                customer.getName(),
                customer.getPrimaryChannel().name(),
                session.getId()
        );
    }

    /**
     * 根据渠道信息查找或创建客户
     * @param channel 渠道类型
     * @param name 客户名称
     * @param email 邮箱（可选）
     * @param phone 手机号（可选）
     * @param channelUserId 渠道用户ID（可选）
     * @return 客户对象
     */
    @Transactional
    public Customer findOrCreateByChannel(Channel channel, String name, String email, String phone, String channelUserId) {
        Customer customer = null;
        
        // 优先根据渠道用户ID查找
        if (channelUserId != null && !channelUserId.isBlank()) {
            customer = switch (channel) {
                case WECHAT -> customerRepository.findByWechatOpenId(channelUserId).orElse(null);
                case WHATSAPP -> customerRepository.findByWhatsappId(channelUserId).orElse(null);
                case LINE -> customerRepository.findByLineId(channelUserId).orElse(null);
                case TELEGRAM -> customerRepository.findByTelegramId(channelUserId).orElse(null);
                case FACEBOOK -> customerRepository.findByFacebookId(channelUserId).orElse(null);
                default -> null;
            };
        }
        
        // 根据邮箱查找
        if (customer == null && email != null && !email.isBlank()) {
            customer = customerRepository.findByEmail(email).orElse(null);
        }
        
        // 根据手机号查找
        if (customer == null && phone != null && !phone.isBlank()) {
            customer = customerRepository.findByPhone(phone).orElse(null);
        }
        
        // 如果找到了客户，更新信息并返回
        if (customer != null) {
            updateCustomerInfo(customer, name, email, phone, channelUserId, channel);
            return customerRepository.save(customer);
        }
        
        // 创建新客户
        customer = new Customer();
        customer.setName(name);
        customer.setPrimaryChannel(channel);
        customer.setEmail(email);
        customer.setPhone(phone);
        
        // 设置渠道用户ID
        if (channelUserId != null && !channelUserId.isBlank()) {
            switch (channel) {
                case WECHAT -> customer.setWechatOpenId(channelUserId);
                case WHATSAPP -> customer.setWhatsappId(channelUserId);
                case LINE -> customer.setLineId(channelUserId);
                case TELEGRAM -> customer.setTelegramId(channelUserId);
                case FACEBOOK -> customer.setFacebookId(channelUserId);
            }
        }
        
        return customerRepository.save(customer);
    }
    
    private void updateCustomerInfo(Customer customer, String name, String email, String phone, String channelUserId, Channel channel) {
        // 更新名称（如果提供了新名称）
        if (name != null && !name.isBlank() && !name.equals(customer.getName())) {
            customer.setName(name);
        }
        
        // 更新邮箱（如果提供了且不冲突）
        if (email != null && !email.isBlank() && customer.getEmail() == null) {
            customer.setEmail(email);
        }
        
        // 更新手机号（如果提供了且不冲突）
        if (phone != null && !phone.isBlank() && customer.getPhone() == null) {
            customer.setPhone(phone);
        }
        
        // 更新渠道用户ID
        if (channelUserId != null && !channelUserId.isBlank()) {
            switch (channel) {
                case WECHAT:
                    if (customer.getWechatOpenId() == null) {
                        customer.setWechatOpenId(channelUserId);
                    }
                    break;
                case WHATSAPP:
                    if (customer.getWhatsappId() == null) {
                        customer.setWhatsappId(channelUserId);
                    }
                    break;
                case LINE:
                    if (customer.getLineId() == null) {
                        customer.setLineId(channelUserId);
                    }
                    break;
                case TELEGRAM:
                    if (customer.getTelegramId() == null) {
                        customer.setTelegramId(channelUserId);
                    }
                    break;
                case FACEBOOK:
                    if (customer.getFacebookId() == null) {
                        customer.setFacebookId(channelUserId);
                    }
                    break;
            }
        }
    }
}
