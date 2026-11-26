package io.github.elmatty18.workflowinbox.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

interface WorkflowStateRepository extends JpaRepository<WorkflowStateEntity, Integer> {

}
