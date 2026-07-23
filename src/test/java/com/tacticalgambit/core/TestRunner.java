package com.tacticalgambit.core;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

public class TestRunner {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                    selectPackage("com.tacticalgambit.core")
                )
                .filters(EngineFilter.excludeEngines("junit-vintage"))
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        var summary = listener.getSummary();
        System.out.println("==================================================");
        System.out.println("TACTICALGAMBIT-CORE JUNIT 5 TEST EXECUTION RESULTS");
        System.out.println("==================================================");
        System.out.println("Tests encontrados : " + summary.getTestsFoundCount());
        System.out.println("Tests exitosos   : " + summary.getTestsSucceededCount());
        System.out.println("Tests fallidos   : " + summary.getTestsFailedCount());
        System.out.println("Tests omitidos   : " + summary.getTestsSkippedCount());
        System.out.println("==================================================");

        if (summary.getTestsFailedCount() > 0) {
            summary.getFailures().forEach(failure -> {
                System.err.println("FALLO: " + failure.getTestIdentifier().getDisplayName());
                failure.getException().printStackTrace();
            });
            System.exit(1);
        }
    }
}
