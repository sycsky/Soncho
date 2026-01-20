package com.example.aikef.service;

import com.example.aikef.dto.RoleDto;
import com.example.aikef.dto.request.CreateRoleRequest;
import com.example.aikef.dto.request.UpdateRoleRequest;
import com.example.aikef.mapper.EntityMapper;
import com.example.aikef.model.Role;
import com.example.aikef.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final EntityMapper entityMapper;

    public RoleService(RoleRepository roleRepository, EntityMapper entityMapper) {
        this.roleRepository = roleRepository;
        this.entityMapper = entityMapper;
    }

    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream()
                .map(entityMapper::toRoleDto)
                .toList();
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());

        // 将 Set<String> 转换为 Map<String, Object>
        if (request.permissions() != null) {
            Map<String, Object> permissionsMap = request.permissions().stream()
                    .collect(Collectors.toMap(
                            perm -> perm,
                            perm -> true
                    ));
            role.setPermissions(permissionsMap);
        }
        
        Role saved = roleRepository.save(role);
        return entityMapper.toRoleDto(saved);
    }

    @Transactional
    public RoleDto updateRole(UUID roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("角色不存在"));
        if (role.isSystem()) {
            throw new IllegalStateException("System roles cannot be modified.");
        }
        role.setName(request.name());
        role.setDescription(request.description());
        if (request.permissions() != null) {
            role.setPermissions(request.permissions());
        }
        return entityMapper.toRoleDto(role);
    }

    @Transactional
    public void deleteRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("角色不存在"));
        if (role.isSystem()) {
            throw new IllegalStateException("System roles cannot be deleted.");
        }
        // TODO: Check if any agent is using this role before deletion
        roleRepository.deleteById(roleId);
    }
}
