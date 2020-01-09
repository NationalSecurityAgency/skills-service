describe('Projects Tests', () => {
  beforeEach(() => {
    cy.server()
      .route('GET', '/app/projects').as('getProjects')
      .route('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
      .route('GET', '/app/userInfo').as('getUserInfo')
  });

  it('Create new projects', function () {
    cy.visit('/');

    cy.route('POST', '/app/projects/MyNewtestProject').as('postNewProject');

    cy.clickButton('Project');
    cy.get('[data-vv-name="projectName"]').type("My New test Project")
    cy.clickSave();

    cy.wait('@postNewProject');

    cy.contains('My New test Project')
    cy.contains('ID: MyNewtestProject')
  });

  it('Duplicate project names are not allowed', () => {
    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
    })
    cy.visit('/');

    cy.clickButton('Project');
    cy.get('[data-vv-name="projectName"]').type("My New test Project")

    cy.contains('The value for the Project Name is already taken')
    cy.clickSave();
    cy.contains('***Form did NOT pass validation, please fix and try to Save again***')
  });


  it('Duplicate project ids are not allowed', () => {
    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
    })
    cy.visit('/');
    cy.clickButton('Project');
    cy.get('[data-vv-name="projectName"]').type("Other Project Name")
    cy.contains('Enable').click();
    cy.getIdField().clear().type("MyNewtestProject")

    cy.contains('The value for the Project ID is already taken')
    cy.clickSave();
    cy.contains('***Form did NOT pass validation, please fix and try to Save again***')
  });

  it('Project id autofill strips out special characters and spaces', () => {
    const expectedId = 'LotsofspecialPchars';
    const providedName = "!L@o#t$s of %s^p&e*c(i)a_l++_|}{P c'ha'rs";

    cy.route('POST', `/app/projects/${expectedId}`).as('postNewProject');

    cy.visit('/');
    cy.clickButton('Project');
    cy.get('[data-vv-name="projectName"]').type(providedName)
    cy.getIdField().should('have.value', expectedId)

    cy.clickSave();
    cy.wait('@postNewProject');

    cy.contains(`ID: ${expectedId}`)
  });

  it('Validate that cannot create project with the same name in lowercase', () => {
    const expectedId = 'TestProject1';
    const providedName = "Test Project #1";

    cy.route('POST', `/app/projects/${expectedId}`)
        .as('postNewProject');

    cy.visit('/');
    cy.clickButton('Project');
    cy.get('[data-vv-name="projectName"]').type(providedName)
    cy.getIdField().should('have.value', expectedId)

    cy.clickSave();
    cy.wait('@postNewProject');

    cy.clickButton('Project');
    cy.get('[data-vv-name="projectName"]').type(providedName.toLowerCase())

    cy.contains('The value for the Project Name is already taken')

    cy.clickSave();
    cy.contains('***Form did NOT pass validation, please fix and try to Save again***')
  });

  it('Once project id is enabled name-to-id autofill should be turned off', () => {
    cy.visit('/');

    cy.clickButton('Project');;
    cy.get('[data-vv-name="projectName"]').type('InitValue');
    cy.getIdField().should('have.value', 'InitValue');

    cy.contains('Enable').click();
    cy.contains('Enabled').not('a');

    cy.get('[data-vv-name="projectName"]').type('MoreValue');
    cy.getIdField().should('have.value', 'InitValue');

    cy.get('[data-vv-name="projectName"]').clear();
    cy.getIdField().should('have.value', 'InitValue');
  });

  it('Project name is required', () => {
    cy.visit('/')
    cy.clickButton('Project');;
    cy.contains('Enable').click();
    cy.getIdField().type('InitValue');

    cy.clickSave();

    cy.contains('The Project Name field is required')
    cy.contains('***Form did NOT pass validation, please fix and try to Save again***')
  })

  it('Project id is required', () => {
    cy.visit('/')
    cy.clickButton('Project');;
    cy.get('[data-vv-name="projectName"]').type('New Project');
    cy.contains('Enable').click();
    cy.getIdField().clear()

    cy.clickSave();

    cy.contains('The Project ID field is required')
    cy.contains('***Form did NOT pass validation, please fix and try to Save again***')
  })


  it('Project name must be > 3 chars < 50 chars', () => {
    const minLenMsg = 'Project Name cannot be less than 3 characters';
    const maxLenMsg = 'Project Name cannot exceed 50 characters';
    const projId = 'ProjectId'
    cy.route('POST', `/app/projects/${projId}`).as('postNewProject');

    cy.visit('/')
    cy.clickButton('Project');;
    cy.contains('Enable').click();
    cy.getIdField().type('ProjectId')
    cy.get('[data-vv-name="projectName"]').type('12');
    cy.contains(minLenMsg)

    cy.get('[data-vv-name="projectName"]').type('3');
    cy.contains(minLenMsg).should('not.exist')

    const longInvalid = Array(51).fill('a').join('');
    const longValid = Array(50).fill('a').join('');

    cy.get('[data-vv-name="projectName"]').clear().type(longInvalid);
    cy.contains(maxLenMsg)

    cy.get('[data-vv-name="projectName"]').clear().type(longValid);
    cy.contains(maxLenMsg).should('not.exist')

    cy.clickSave();
    cy.wait('@postNewProject');

    cy.contains(`ID: ${projId}`)
    cy.contains(longValid)
  })

  it('Project ID must be > 3 chars < 50 chars', () => {
    const minLenMsg = 'Project ID cannot be less than 3 characters';
    const maxLenMsg = 'Project ID cannot exceed 50 characters';
    const projName = 'Project Name'

    const longInvalid = Array(51).fill('a').join('');
    const longValid = Array(50).fill('a').join('');
    cy.route('POST', `/app/projects/${longValid}`).as('postNewProject');

    cy.visit('/')
    cy.clickButton('Project');;
    cy.contains('Enable').click();
    cy.getIdField().type('12')
    cy.get('[data-vv-name="projectName"]').type(projName);
    cy.contains(minLenMsg)

    cy.getIdField().type('3');
    cy.contains(minLenMsg).should('not.exist')

    cy.getIdField().clear().type(longInvalid);
    cy.contains(maxLenMsg)

    cy.getIdField().clear().type(longValid);
    cy.contains(maxLenMsg).should('not.exist')

    cy.clickSave();
    cy.wait('@postNewProject');

    cy.contains('ID: aaaaa')
  })


});
