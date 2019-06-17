create table skills.project_definition
(
	id int not null AUTO_INCREMENT PRIMARY KEY,
	project_id varchar(255) not null,
	name varchar(255) not null,
    client_secret varchar(255) not null,
	total_points int not null default 0,
	display_order int not null,

	INDEX (project_id),
	CONSTRAINT ProjectDefUniqueProjectIdConstraint UNIQUE (project_id),
  CONSTRAINT ProjectDefUniqueProjectNameConstraint UNIQUE (name),

	created DATETIME default CURRENT_TIMESTAMP,
	updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;

create table skills.custom_icons
(
	id int not null AUTO_INCREMENT PRIMARY KEY,
  project_id varchar(255),
  proj_ref_id int,
  filename varchar(255) not null,
  content_type varchar(255) not null,
  data_uri text not null,

  INDEX project_id_ind (project_id),
  INDEX filename_ind (filename),
  CONSTRAINT UNIQUE(project_id, filename),

  FOREIGN KEY (project_id) REFERENCES skills.project_definition(project_id) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (proj_ref_id) REFERENCES skills.project_definition(id) ON UPDATE CASCADE ON DELETE CASCADE,

  created DATETIME default CURRENT_TIMESTAMP,
	updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;

create table skills.skill_definition
(
  id int not null AUTO_INCREMENT PRIMARY KEY,
 	skill_id varchar(255) not null,
	project_id varchar(255) not null,
	custom_icon_id int,
	proj_ref_id int, /** optional, only if immediate child of project **/
	name varchar(255) not null,
	point_increment int not null default 100,
	point_increment_interval int not null default 8,
	total_points int not null default 200,
	description text null,
	help_url text null,
	display_order int not null,
	type varchar(255) not null,
	icon_class varchar(255),
	start_date DATETIME, /* optional, only used for 'gem' badges currently */
	end_date DATETIME, /* optional, only used for 'gem' badges currently */
	version int not null default 0,

  INDEX (skill_id),
	INDEX (project_id),

	CONSTRAINT SkillDefUniqueSkillIdConstraint UNIQUE (project_id, skill_id, type),
	CONSTRAINT SkillDefUniqueSkillNameConstraint UNIQUE (project_id, name, type),

	FOREIGN KEY (project_id) REFERENCES skills.project_definition(project_id) ON UPDATE CASCADE ON DELETE CASCADE,
	FOREIGN KEY (proj_ref_id) REFERENCES skills.project_definition(id) ON UPDATE CASCADE ON DELETE CASCADE,
	FOREIGN KEY (custom_icon_id) REFERENCES skills.custom_icons(id) ON UPDATE CASCADE ON DELETE CASCADE,

  created DATETIME default CURRENT_TIMESTAMP,
  updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;

create table skills.skill_relationship_definition
(
  id int not null AUTO_INCREMENT PRIMARY KEY,
 	parent_id int not null,
	child_id int not null,
	type varchar(255) not null,

  INDEX (parent_id),
  INDEX (child_id),

  FOREIGN KEY (parent_id) REFERENCES skills.skill_definition(id) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (child_id) REFERENCES skills.skill_definition(id) ON UPDATE CASCADE ON DELETE CASCADE,

  CONSTRAINT UNIQUE (parent_id, child_id, type),

  created DATETIME default CURRENT_TIMESTAMP,
  updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;


create table skills.skill_share_definition
(
  id int not null AUTO_INCREMENT PRIMARY KEY,
  skill_id int not null,
  shared_to_project_id int not null,

  INDEX (skill_id),
  INDEX (shared_to_project_id),

  FOREIGN KEY (skill_id) REFERENCES skills.skill_definition(id) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (shared_to_project_id) REFERENCES skills.project_definition(id) ON UPDATE CASCADE ON DELETE CASCADE,

  CONSTRAINT UNIQUE (skill_id, shared_to_project_id),

  created DATETIME default CURRENT_TIMESTAMP,
  updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;

create table skills.level_definition
(
  id int not null AUTO_INCREMENT PRIMARY KEY,
  /** optional, either belong to project or skill **/
  project_id int,
	skill_id int,

	level int not null,
	percent int,

	INDEX (project_id),
	INDEX (skill_id),

	FOREIGN KEY (skill_id) REFERENCES skills.skill_definition(id) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (project_id) REFERENCES skills.project_definition(id) ON DELETE CASCADE ON UPDATE CASCADE,

	created DATETIME default CURRENT_TIMESTAMP,
  updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  custom_icon_id int,
  icon_class varchar(255),
  logical_name varchar(255),
  points_from int,
  points_to int
) engine=InnoDB;

create table skills.users
(
	id int not null AUTO_INCREMENT PRIMARY KEY,
	user_id varchar(255) not null,
	password varchar(255),

	INDEX (user_id),

	CONSTRAINT UNIQUE (user_id),

	created DATETIME default CURRENT_TIMESTAMP,
	updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;

create table skills.user_roles
(
	id int not null AUTO_INCREMENT PRIMARY KEY,
	user_ref_id int not null,
	user_id varchar(255) not null,
	role_name varchar(255) not null,
  project_id varchar(255),

	INDEX (user_id),

	CONSTRAINT UNIQUE (user_id, role_name, project_id),

  FOREIGN KEY (user_ref_id) REFERENCES skills.users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (user_id) REFERENCES skills.users(user_id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (project_id) REFERENCES skills.project_definition(project_id) ON DELETE CASCADE ON UPDATE CASCADE,

	created DATETIME default CURRENT_TIMESTAMP,
	updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;

create table skills.user_properties
(
	id int not null AUTO_INCREMENT PRIMARY KEY,
	user_id int not null,
	name varchar(255) not null,
	value varchar(255) not null,

	INDEX (user_id),

	CONSTRAINT UNIQUE (name, value, user_id),

  FOREIGN KEY (user_id) REFERENCES skills.users(id) ON DELETE CASCADE ON UPDATE CASCADE,

	created DATETIME default CURRENT_TIMESTAMP,
	updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;

create table skills.user_performed_skill
(
  id int not null AUTO_INCREMENT PRIMARY KEY,
  user_id varchar(255) not null,
  skill_id varchar(255) not null,
  project_id varchar(255) not null,
  performed_on DATETIME not null,

  INDEX (skill_id),
  INDEX (project_id),
  INDEX (project_id, skill_id),

  FOREIGN KEY (project_id) REFERENCES skills.project_definition(project_id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (project_id, skill_id) REFERENCES skills.skill_definition(project_id, skill_id) ON DELETE CASCADE ON UPDATE CASCADE,

  created DATETIME default CURRENT_TIMESTAMP,
  updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;


create table skills.user_points
(
  id int not null AUTO_INCREMENT PRIMARY KEY,
  user_id varchar(255) not null,
  project_id varchar(255) not null,
  skill_id varchar(255) null,
  points int not null,

--   Optional: documents which day points are for; null indicates overall points rather than a single day
  day DATE,

  INDEX (project_id),
  INDEX (project_id, skill_id),
  INDEX (user_id, project_id, skill_id, day),

  CONSTRAINT UNIQUE (user_id, project_id, skill_id, day),

  FOREIGN KEY (project_id) REFERENCES skills.project_definition(project_id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (project_id, skill_id) REFERENCES skills.skill_definition(project_id, skill_id) ON DELETE CASCADE ON UPDATE CASCADE,

  created DATETIME default CURRENT_TIMESTAMP,
  updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP

) engine=InnoDB;

create table skills.user_achievement
(
  id int not null AUTO_INCREMENT PRIMARY KEY,

  user_id varchar(255) not null,
  project_id varchar(255) not null,
  skill_id varchar(255) null,
  skill_ref_id int null,

  level int,
  points_when_achieved int not null,

  INDEX (project_id),
  INDEX (skill_id),
  INDEX (project_id, skill_id),

  FOREIGN KEY (skill_ref_id) REFERENCES skills.skill_definition(id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (project_id) REFERENCES skills.project_definition(project_id) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (project_id, skill_id) REFERENCES skills.skill_definition(project_id, skill_id) ON DELETE CASCADE ON UPDATE CASCADE,

  created DATETIME default CURRENT_TIMESTAMP,
  updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;

create table skills.allowed_origins
(
	id int not null AUTO_INCREMENT PRIMARY KEY,
	allowed_origin varchar(255) not null,
  project_id varchar(255),

  INDEX user_id_ind (project_id),
  CONSTRAINT UNIQUE (project_id, allowed_origin),
  FOREIGN KEY (project_id) REFERENCES skills.project_definition(project_id) ON DELETE CASCADE ON UPDATE CASCADE,

	created DATETIME default CURRENT_TIMESTAMP,
	updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;

create table skills.settings
(
  id int not null AUTO_INCREMENT PRIMARY KEY,
  setting varchar(255) not null,
  value varchar(255) not null,
  project_id varchar(255),

  setting_group varchar(255),

  INDEX (project_id),
  INDEX (setting_group),
  INDEX (setting),
  INDEX (project_id, setting),
  INDEX (project_id, setting_group),

  UNIQUE(project_id, setting),
  FOREIGN KEY (project_id) REFERENCES skills.project_definition(project_id) ON DELETE CASCADE ON UPDATE CASCADE,

  created DATETIME default CURRENT_TIMESTAMP,
  updated DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) engine=InnoDB;
