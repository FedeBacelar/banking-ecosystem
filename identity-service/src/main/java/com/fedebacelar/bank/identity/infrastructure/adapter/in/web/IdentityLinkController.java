package com.fedebacelar.bank.identity.infrastructure.adapter.in.web;

import com.fedebacelar.bank.identity.application.port.in.ChangeIdentityLinkStatusUseCase;
import com.fedebacelar.bank.identity.application.port.in.CreateIdentityLinkUseCase;
import com.fedebacelar.bank.identity.application.port.in.GetCustomerIdentityLinksUseCase;
import com.fedebacelar.bank.identity.application.port.in.ResolveIdentityLinkUseCase;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import com.fedebacelar.bank.identity.infrastructure.adapter.in.web.dto.CreateIdentityLinkRequest;
import com.fedebacelar.bank.identity.infrastructure.adapter.in.web.dto.IdentityLinkResponse;
import com.fedebacelar.bank.identity.infrastructure.adapter.in.web.mapper.IdentityLinkWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identity-links")
@Validated
public class IdentityLinkController {

    private final CreateIdentityLinkUseCase createIdentityLinkUseCase;
    private final ResolveIdentityLinkUseCase resolveIdentityLinkUseCase;
    private final GetCustomerIdentityLinksUseCase getCustomerIdentityLinksUseCase;
    private final ChangeIdentityLinkStatusUseCase changeIdentityLinkStatusUseCase;
    private final IdentityLinkWebMapper mapper;

    public IdentityLinkController(
            CreateIdentityLinkUseCase createIdentityLinkUseCase,
            ResolveIdentityLinkUseCase resolveIdentityLinkUseCase,
            GetCustomerIdentityLinksUseCase getCustomerIdentityLinksUseCase,
            ChangeIdentityLinkStatusUseCase changeIdentityLinkStatusUseCase,
            IdentityLinkWebMapper mapper
    ) {
        this.createIdentityLinkUseCase = createIdentityLinkUseCase;
        this.resolveIdentityLinkUseCase = resolveIdentityLinkUseCase;
        this.getCustomerIdentityLinksUseCase = getCustomerIdentityLinksUseCase;
        this.changeIdentityLinkStatusUseCase = changeIdentityLinkStatusUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Create identity link")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityLinkResponse create(@Valid @RequestBody CreateIdentityLinkRequest request) {
        return mapper.toResponse(createIdentityLinkUseCase.create(mapper.toCommand(request)));
    }

    @Operation(summary = "Resolve active identity link")
    @GetMapping("/providers/{provider}/subjects/{providerSubject}")
    public IdentityLinkResponse resolveActive(
            @PathVariable IdentityProvider provider,
            @PathVariable @NotBlank @Size(max = 255) String providerSubject
    ) {
        return mapper.toResponse(resolveIdentityLinkUseCase.resolveActive(provider, providerSubject));
    }

    @Operation(summary = "Get identity links by customer")
    @GetMapping("/customers/{customerId}")
    public List<IdentityLinkResponse> getByCustomerId(@PathVariable UUID customerId) {
        return getCustomerIdentityLinksUseCase.getByCustomerId(customerId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Operation(summary = "Activate identity link")
    @PatchMapping("/{identityLinkId}/activate")
    public IdentityLinkResponse activate(@PathVariable UUID identityLinkId) {
        return mapper.toResponse(changeIdentityLinkStatusUseCase.activate(identityLinkId));
    }

    @Operation(summary = "Disable identity link")
    @PatchMapping("/{identityLinkId}/disable")
    public IdentityLinkResponse disable(@PathVariable UUID identityLinkId) {
        return mapper.toResponse(changeIdentityLinkStatusUseCase.disable(identityLinkId));
    }
}
