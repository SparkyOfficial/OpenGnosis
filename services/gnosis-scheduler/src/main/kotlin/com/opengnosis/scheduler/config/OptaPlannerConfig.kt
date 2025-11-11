package com.opengnosis.scheduler.config

import com.opengnosis.scheduler.optaplanner.ScheduleConstraintProvider
import com.opengnosis.scheduler.optaplanner.ScheduleSolution
import org.optaplanner.core.api.solver.SolverFactory
import org.optaplanner.core.api.solver.SolverManager
import org.optaplanner.core.config.solver.SolverConfig
import org.optaplanner.core.config.solver.termination.TerminationConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.UUID

@Configuration
class OptaPlannerConfig {
    
    @Bean
    fun solverFactory(): SolverFactory<ScheduleSolution> {
        val solverConfig = SolverConfig()
            .withSolutionClass(ScheduleSolution::class.java)
            .withEntityClasses(com.opengnosis.scheduler.optaplanner.SchedulePlanningEntity::class.java)
            .withConstraintProviderClass(ScheduleConstraintProvider::class.java)
            .withTerminationConfig(
                TerminationConfig()
                    .withSpentLimit(Duration.ofSeconds(30))
            )
        
        return SolverFactory.create(solverConfig)
    }
    
    @Bean
    fun solverManager(solverFactory: SolverFactory<ScheduleSolution>): SolverManager<ScheduleSolution, UUID> {
        return SolverManager.create(solverFactory)
    }
}
