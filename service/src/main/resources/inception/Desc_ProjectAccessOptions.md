The Project Access page supports adding or removing Project Administrators, Project Approvers, and inviting users to join a project if the project has been configured as an Invite Only project as well as revoking a user's access.

To add and remove project Administrators and Approvers navigate to `Project -> Access` page.

You must have an Admin role in order to manage other Admin and/or Approver users for a project. There supported project roles are:

* <strong>Admin</strong>: enables management of the training profile for that project such as creating and modifying subjects, skills, badges, etc.
* <strong>Approver</strong>: allowed to approve and deny Self Reporting approval requests while only getting a read-only view of the project.

### Invite Only

If the project has been configured with a visibility of `Private Invite Only`, invite and access revocation are controlled here.

> **TIP**
> The `Project User: Invite` and `Project User: Revoke` user interface controls are only displayed if the project has been configured with a Visibility of Invite Only

#### Invite Users

Users are invited to join a project that has been configured as `Private Invite Only` via email. Invite recipients are added via the `Email Addresses` input field and can be added one at a time or by using a comma or semicolon separated list or by entering one email address per line in the `Email Addresses` input field (email addresses in the form of ` Firstname Lastname` are also supported). Once email addresses have been entered into the `Email Addresses` input field, they must be added as recipients using the `Add Recipients` button. These steps may be performed several times before the `Send Invites` button is pressed at which point all accumulated email addresses will be sent an email with a unique, one-time use invite to join the project.

Project invites may be configured to expire if not accepted within a certain time frame. Please note that the expiration configuration is only applicable to how long the invite code itself is valid. Once a user has accepted a valid invite code and joined the project, the invite expiration configuration is no longer relevant for that user.

> **TIP**
> Each email recipient receives a unique project invite code, therefore distribution lists or group email accounts should not be added as recipients as only the first person to claim the invite will be able to use it.

##### Manage Invites Pending Acceptance

Once Project Invites have been sent to users, any invites that have not yet been accepted or invites that have recently expired can be managed through the `Invites Pending Acceptance` table below the `Invite Users` form.

Once an invite has been accepted by a user, it will no longer be displayed in the `Invites Pending Acceptance` table, in that case the user will appear in the Revoke Access table.

The actions that can be performed on each invite are: expiration time extension, remind user of invite, and delete invite.

`Expiration Time Extension` can be performed for any invite listed in the `Invites Pending Acceptance` table. For invites that are not yet expired, the selected extension time will be added to the invite's current expiration time. For invites that have expired, the selected extension time will be added to the time at which the extension is performed. For example, if an invite expires in 15 minutes and it is extended by 30 minutes, it will expire in 45 minutes. If an invite is currently expired and is extended by 30 minutes, it will expire in 30 minutes.

`Remind User of Invite` will send a reminder email to the user, encouraging the user to accept the project invite. It is important to note that a reminder cannot be sent for an invite that has expired. If an invite has expired, it must first be given an extension at which point the notification button will become enabled and a reminder may be sent to the user.

`Delete invite` will cause the selected project invite to be deleted, any future attempts to use that invite code will fail. Note that a new invite can be generated for the user whose invite was deleted if desired. Expired invites may be deleted to remove them from the view prior to removal by automated cleanup processes.

> **TIP**
> Expired invites are, by default, visible for 30 days after they have expired; however, this is a configurable, system level, property that may be configured differently depending on the environment

#### Revoke Access

Once a user has accepted an invitation to join a project configured for Invite Only visibility, that user will show up under the Revoke Access table at which point their access can be revoked and they will no longer have access to the project.

> **TIP**
> When a user's access to an Invite Only project has been revoked, only that user's access is removed. Their achievement history is retained in case they are granted access in the future or the project's visibility is changed.

## How To Earn Points

To earn points for this skill please read about the different Access Options above and then click `I did it` button below.