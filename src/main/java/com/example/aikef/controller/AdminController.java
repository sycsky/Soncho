package com.example.aikef.controller;

import com.example.aikef.dto.AgentDto;
import com.example.aikef.dto.RoleDto;
import com.example.aikef.dto.request.CreateAgentRequest;
import com.example.aikef.dto.request.CreateRoleRequest;
import com.example.aikef.dto.request.UpdateAgentRequest;
import com.example.aikef.dto.request.UpdateRoleRequest;
import com.example.aikef.service.AgentService;
import com.example.aikef.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AgentService agentService;
    private final RoleService roleService;

    public AdminController(AgentService agentService, RoleService roleService) {
        this.agentService = agentService;
        this.roleService = roleService;
    }

    // --- Agent Management ---
    @GetMapping("/agents")
    public Page<AgentDto> listAgents(@RequestParam(required = false) String name,
                                     @RequestParam(required = false) String role,
                                     Pageable pageable) {
        return agentService.listAgents(name, role, pageable);
    }

    @PostMapping("/agents")
    @ResponseStatus(HttpStatus.CREATED)
    public AgentDto createAgent(@Valid @RequestBody CreateAgentRequest request) {
        return agentService.createAgent(request);
    }

    @PutMapping("/agents/{id}")
    public AgentDto updateAgent(@PathVariable UUID id, @Valid @RequestBody UpdateAgentRequest request) {
        return agentService.updateAgent(id, request);
    }

    @DeleteMapping("/agents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAgent(@PathVariable UUID id) {
        agentService.deleteAgent(id);
    }

    // --- Role Management ---
    @GetMapping("/roles")
    public List<RoleDto> listRoles() {
        return roleService.listRoles();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDto createRole(@Valid @RequestBody CreateRoleRequest request) {
        return roleService.createRole(request);
    }

    @PutMapping("/roles/{id}")
    public RoleDto updateRole(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return roleService.updateRole(id, request);
    }

    @DeleteMapping("/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
    }
}
