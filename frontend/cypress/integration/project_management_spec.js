describe('Project Tests', () => {

  beforeEach(() => {
    cy.server()
      .route('GET', '/app/projects').as('getProjects')
      .route('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
      .route('GET', '/app/userInfo').as('getUserInfo')

    cy.visit('/');
    cy.get('#username').type('skills@skills.org');
    cy.get('#inputPassword').type('p@ssw0rd');
    cy.contains('Login').click();
    cy.wait('@getProjects');
    cy.wait('@getUserInfo');

    // cy.get('.card-body').each((value, index , collection) => {
    //   cy.wrap(value).get('button').click()
    // })
  });

  it('Create new projects', () => {
    cy.route('POST', '/app/projects/MyNewtestProject').as('postNewProject');

    cy.get('button:contains(\'Project\')').click()
    cy.get('[data-vv-name="projectName"]').type("My New test Project")
    cy.get("button:contains('Save')").click()

    cy.wait('@postNewProject');

    cy.contains('My New test Project')
    cy.contains('ID: MyNewtestProject')
  });

  it('Duplicate project names are not allowed', () => {
    cy.get('button:contains(\'Project\')').click()
    cy.get('[data-vv-name="projectName"]').type("My New test Project")

    cy.contains('The value for the Project Name is already taken')
    cy.get("button:contains('Save')").click()
    cy.contains('***Form did NOT pass validation, please fix and try to Save again***')
  });


  it('Duplicate project ids are not allowed', () => {
    cy.get('button:contains(\'Project\')').click()
    cy.get('[data-vv-name="projectName"]').type("Other Project Name")
    cy.contains('Enable').click();
    cy.get("#idInput").clear().type("MyNewtestProject")

    cy.contains('The value for the Project ID is already taken')
    cy.get("button:contains('Save')").click()
    cy.contains('***Form did NOT pass validation, please fix and try to Save again***')
  });

});
