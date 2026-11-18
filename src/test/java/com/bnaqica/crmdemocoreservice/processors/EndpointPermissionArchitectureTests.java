package com.bnaqica.crmdemocoreservice.processors;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnaqica.crmdemocoreservice.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Fails the build when any endpoint of the service doesn't declare {@code @RequiresPermission}. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EndpointPermissionArchitectureTests {

    @Autowired
    private DeclaredPermissions declared;

    @Test
    void everyEndpointDeclaresAPermission() {
        assertThat(declared.undeclaredEndpoints())
                .as("endpoints without @RequiresPermission")
                .isEmpty();
    }

    @Test
    void declaredPermissionsAgreeOnDescriptionAndSensitivity() {
        assertThat(declared.catalog()).isNotNull();
    }
}
