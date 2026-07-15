package com.dbwb.platform.portfolio;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.portfolio.dto.ServiceItemRequest;
import com.dbwb.platform.portfolio.entity.ServiceItem;
import com.dbwb.platform.portfolio.repository.ServiceItemRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceItemServiceTest {

    @Mock
    private WebsiteAccessGuard accessGuard;
    @Mock
    private ServiceItemRepository repository;

    private ServiceItemService serviceItemService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount caller = new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        serviceItemService = new ServiceItemService(accessGuard, repository);
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        lenient().when(accessGuard.requirePermission(eq(websiteId), eq(caller), any())).thenReturn(website);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    @Test
    void createAssignsTheNextSortOrder() {
        when(repository.countByWebsiteId(websiteId)).thenReturn(2L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var service = serviceItemService.create(websiteId, caller,
                new ServiceItemRequest("Haircut", "A trim", new BigDecimal("15.00"), null));

        assertThat(service.getSortOrder()).isEqualTo(2);
        assertThat(service.getName()).isEqualTo("Haircut");
        assertThat(service.getPrice()).isEqualByComparingTo("15.00");
    }

    @Test
    void createAllowsANullPriceForServicesPricedOnRequest() {
        when(repository.countByWebsiteId(websiteId)).thenReturn(0L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var service = serviceItemService.create(websiteId, caller, new ServiceItemRequest("Consultation", null, null, null));

        assertThat(service.getPrice()).isNull();
    }

    @Test
    void updateRejectsAServiceBelongingToAnotherWebsite() {
        ServiceItem foreign = new ServiceItem();
        foreign.setWebsite(TestEntities.withId(new BusinessWebsite(), UUID.randomUUID()));
        UUID serviceId = UUID.randomUUID();
        when(repository.findById(serviceId)).thenReturn(Optional.of(TestEntities.withId(foreign, serviceId)));

        assertThatThrownBy(() -> serviceItemService.update(websiteId, serviceId, caller,
                new ServiceItemRequest("New name", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reorderAppliesTheGivenOrderToEachService() {
        ServiceItem first = itemWithId();
        ServiceItem second = itemWithId();
        when(repository.findById(first.getId())).thenReturn(Optional.of(first));
        when(repository.findById(second.getId())).thenReturn(Optional.of(second));

        serviceItemService.reorder(websiteId, caller, List.of(second.getId(), first.getId()));

        assertThat(second.getSortOrder()).isZero();
        assertThat(first.getSortOrder()).isEqualTo(1);
    }

    private ServiceItem itemWithId() {
        ServiceItem item = new ServiceItem();
        item.setWebsite(website);
        return TestEntities.withId(item, UUID.randomUUID());
    }
}
