describe('Project Tests', () => {

  beforeEach(() => {
    // this is workaround, because command fails the very first time,
    cy.exec('npm version', {failOnNonZeroExit: false})
    cy.exec('npm run backend:clearDb')
    cy.request('PUT', '/createAccount', {
      firstName: 'Person',
      lastName : 'OneTwo',
      email    : 'root@skills.org',
      password : 'password',
    })
    cy.request('POST', '/grantFirstRoot');
    cy.request('POST', '/logout');

    cy.request('PUT', '/createAccount', {
      firstName: 'Person',
      lastName : 'Three',
      email    : 'skills@skills.org',
      password : 'password',
    })

    cy.server()
      .route('GET', '/app/projects').as('getProjects')
      .route('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
      .route('GET', '/app/userInfo').as('getUserInfo')
  });

  it('Create new projects', () => {
    cy.visit('/');

    cy.route('POST', '/app/projects/MyNewtestProject').as('postNewProject');

    cy.get('button:contains(\'Project\')').click()
    cy.get('[data-vv-name="projectName"]').type("My New test Project")
    cy.get("button:contains('Save')").click()

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

    cy.get('button:contains(\'Project\')').click()
    cy.get('[data-vv-name="projectName"]').type("My New test Project")

    cy.contains('The value for the Project Name is already taken')
    cy.get("button:contains('Save')").click()
    cy.contains('***Form did NOT pass validation, please fix and try to Save again***')
  });


  it('Duplicate project ids are not allowed', () => {
    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
    })
    cy.visit('/');
    cy.get('button:contains(\'Project\')').click()
    cy.get('[data-vv-name="projectName"]').type("Other Project Name")
    cy.contains('Enable').click();
    cy.get("#idInput").clear().type("MyNewtestProject")

    cy.contains('The value for the Project ID is already taken')
    cy.get("button:contains('Save')").click()
    cy.contains('***Form did NOT pass validation, please fix and try to Save again***')
  });

});
