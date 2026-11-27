-- Update the existing menu rows in place so bookmarks and role assignments keep working.
UPDATE t_menu SET title='我的申请', name='MyApplications', component='MyApplications'
WHERE component='WorkflowInstances' AND del_flag=0;

UPDATE t_menu SET title='我的审批', name='MyApprovals', component='MyApprovals'
WHERE component='WorkflowTasks' AND del_flag=0;

UPDATE t_permission SET name='我的申请'
WHERE code IN ('workflow:process','workflow:instance') AND type='menu' AND del_flag=0;

UPDATE t_permission SET name='我的审批'
WHERE code='workflow:task' AND type='menu' AND del_flag=0;
