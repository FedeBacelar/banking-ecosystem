package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.identity.dto.ProvisionedIdentityResponse;
import feign.FeignException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityProvisioningAdapterTest {
    private final IdentityFeignClient client = mock(IdentityFeignClient.class);
    private final IdentityProvisioningAdapter adapter = new IdentityProvisioningAdapter(client);

    @Test
    void recoversAnIdentityAlreadyLinkedToTheSameCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        FeignException.Conflict conflict = mock(FeignException.Conflict.class);
        when(client.create(any())).thenThrow(conflict);
        when(client.getBySubject("subject-1")).thenReturn(new ProvisionedIdentityResponse(
                identityId, customerId, "KEYCLOAK", "subject-1", "ACTIVE"
        ));

        assertThat(adapter.createOrResolve(customerId, "subject-1")).isEqualTo(identityId);
    }

    @Test
    void preservesTheOriginalConflictWhenTheSubjectDoesNotExist() {
        UUID customerId = UUID.randomUUID();
        FeignException.Conflict conflict = mock(FeignException.Conflict.class);
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(client.create(any())).thenThrow(conflict);
        when(client.getBySubject("new-subject")).thenThrow(notFound);

        assertThatThrownBy(() -> adapter.createOrResolve(customerId, "new-subject"))
                .isSameAs(conflict);
    }
}
