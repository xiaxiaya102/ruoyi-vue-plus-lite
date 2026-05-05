-- 修正已有菜单权限标识
u_id = 11620;
GO
u_id = 11621;
GO
u_id = 11629;
GO

-- 流程定义管理相关按钮
:query', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
:add', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
:edit', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
:remove', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
:export', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
:import', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
:publish', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
:copy', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
:active', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO

-- 流程实例管理相关按钮
ce:query', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
ce:variableQuery', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
ce:variable', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
ce:active', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
ce:remove', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
valid', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
cel', N'#', 103, 1, GETDATE(), NULL, NULL, N'');
GO
