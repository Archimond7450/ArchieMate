describe("Navigation", () => {
  it("Appbar Logo navigates to /", () => {
    cy.get("[data-cy='appbar-logo'").click();
    cy.location().should((loc) => {
      expect(loc.pathname).to.equal("/");
    });
  });
  it("Appbar Home icon navigates to /", () => {
    cy.get("[data-cy='appbar-home-icon'").click();
    cy.location().should((loc) => {
      expect(loc.pathname).to.equal("/");
    });
  });
  it("Appbar Home link navigates to /", () => {
    cy.get("[data-cy='appbar-home'").click();
    cy.location().should((loc) => {
      expect(loc.pathname).to.equal("/");
    });
  });
  it("Appbar Docs icon navigates to /docs", () => {
    cy.get("[data-cy='appbar-docs-icon'").click();
    cy.location().should((loc) => {
      expect(loc.pathname).to.equal("/docs");
    });
  });
  it("Appbar Docs link navigates to /docs", () => {
    cy.get("[data-cy='appbar-docs'").click();
    cy.location().should((loc) => {
      expect(loc.pathname).to.equal("/docs");
    });
  });
});
