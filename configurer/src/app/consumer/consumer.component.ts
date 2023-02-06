import {Component, OnInit} from '@angular/core';
import {EnvironmentService} from "../environment.service";
import {DataSourceConfig, Permission, Role} from "../models/config";

@Component({
  selector: 'app-consumer',
  templateUrl: './consumer.component.html',
  styleUrls: ['./consumer.component.css']
})
export class ConsumerComponent implements OnInit {

  constructor(public envService: EnvironmentService) { }

  ngOnInit(): void {
  }

  driverClassNameOptions = [
    'org.h2.Driver',
    'org.postgresql.Driver',
    'com.mysql.cj.jdbc.Driver'
  ];

  dialectOptions = [
    'org.hibernate.dialect.H2Dialect'
  ];

  initDataSource() {
    this.envService.consumerConfig.consumer.dataSource = new DataSourceConfig();
    this.envService.saveEnvEvent.next(null);
  }

  addPermission() {
    this.envService.consumerConfig.consumer.permissions = this.envService.consumerConfig.consumer.permissions || [];
    this.envService.consumerConfig.consumer.permissions.push(new Permission());
    this.envService.saveEnvEvent.next(null);
  }

  getRolePermission(role: Role, permission: 'read'|'write') {
    return !!(role.permission || []).find(p => p === permission);
  }

  setRolePermission(role: Role, permission: 'read'|'write', value: boolean) {
    if (this.getRolePermission(role, permission) === value) {
      return;
    }

    role.permission = role.permission || [];

    if (value) {
      role.permission.push(permission);
    } else {
      role.permission.splice(role.permission.indexOf(permission), 1);
    }
    this.envService.saveEnvEvent.next(null);
  }

  addRole(permission: Permission) {
    permission.roles = permission.roles || [];
    const newRole = new Role();
    newRole.name = 'default';
    newRole.permission = ['read', 'write'];
    permission.roles.push(newRole);
    this.envService.saveEnvEvent.next(null);
  }

}
