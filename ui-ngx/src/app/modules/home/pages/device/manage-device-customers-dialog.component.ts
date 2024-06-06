///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceService } from '@core/http/device.service';
import { forkJoin, Observable } from 'rxjs';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export type ManageDeviceCustomersActionType = 'assign' | 'manage' | 'unassign';

export interface ManageDeviceCustomersDialogData {
  actionType: ManageDeviceCustomersActionType;
  devicesIds: Array<string>;
  assignedCustomersIds?: Array<string>;
}

@Component({
  selector: 'tb-manage-device-customers-dialog',
  templateUrl: './manage-device-customers-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ManageDeviceCustomersDialogComponent}],
  styleUrls: []
})
export class ManageDeviceCustomersDialogComponent extends
  DialogComponent<ManageDeviceCustomersDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  deviceCustomersFormGroup: UntypedFormGroup;

  submitted = false;

  entityType = EntityType;

  titleText: string;
  labelText: string;
  actionName: string;

  assignedCustomersIds: string[];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageDeviceCustomersDialogData,
              private deviceService: DeviceService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ManageDeviceCustomersDialogComponent, boolean>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    this.assignedCustomersIds = data.assignedCustomersIds || [];
    switch (data.actionType) {
      case 'assign':
        this.titleText = 'device.assign-to-customers';
        this.labelText = 'device.assign-to-customers-text';
        this.actionName = 'action.assign';
        break;
      case 'manage':
        this.titleText = 'device.manage-assigned-customers';
        this.labelText = 'device.assigned-customers';
        this.actionName = 'action.update';
        break;
      case 'unassign':
        this.titleText = 'device.unassign-from-customers';
        this.labelText = 'device.unassign-from-customers-text';
        this.actionName = 'action.unassign';
        break;
    }
  }

  ngOnInit(): void {
    this.deviceCustomersFormGroup = this.fb.group({
      assignedCustomerIds: [[...this.assignedCustomersIds]]
    });
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  submit(): void {
    this.submitted = true;
    const customerIds: Array<string> = this.deviceCustomersFormGroup.get('assignedCustomerIds').value;
    const tasks: Observable<any>[] = [];

    this.data.devicesIds.forEach(
      (deviceId) => {
        tasks.push(this.getManageDeviceCustomersTask(deviceId, customerIds));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }

  private getManageDeviceCustomersTask(deviceId: string, customerIds: Array<string>): Observable<any> {
    switch (this.data.actionType) {
      case 'assign':
        return this.deviceService.addDeviceCustomers(deviceId, customerIds);
      case 'manage':
        return this.deviceService.updateDeviceCustomers(deviceId, customerIds);
      case 'unassign':
        return this.deviceService.removeDeviceCustomers(deviceId, customerIds);
    }
  }
}
