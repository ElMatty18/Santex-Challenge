package io.github.elmatty18.workflowinbox.actions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface ActionRepository extends JpaRepository<Action, Long>, JpaSpecificationExecutor<Action> {

}
