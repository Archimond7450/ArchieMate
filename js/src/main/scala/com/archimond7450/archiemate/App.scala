package com.archimond7450.archiemate

import com.archimond7450.archiemate.CirceConfiguration.frontendConfiguration
import com.archimond7450.archiemate.build.BuildInfo
import com.archimond7450.archiemate.elements.NavLinks.*
import com.archimond7450.archiemate.elements.SVGs.menuSvg
import com.archimond7450.archiemate.helpers.FetchHelpers.checkLoginStatus
import com.archimond7450.archiemate.http.User.UserResponse
import com.archimond7450.archiemate.models.{AuthModel, MobileMenuModel}
import com.archimond7450.archiemate.pages.{BroadcasterCommandsPage, DashboardPage, DocsPage, HomePage, LoginPage, LogoutPage, NotFoundPage, OverlayPage}
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.*
import org.scalajs.dom

import scala.concurrent.ExecutionContext.global
import scala.util.{Failure, Success}

object App {
  sealed abstract class Page(val title: String)
  sealed abstract class PageWithHeaderAndFooter(override val title: String) extends Page(title)
  case object Home extends PageWithHeaderAndFooter("Home")
  case object Docs extends PageWithHeaderAndFooter("Docs")
  case object Login extends PageWithHeaderAndFooter("Login")
  case object Logout extends PageWithHeaderAndFooter("Logout")
  case object Dashboard extends PageWithHeaderAndFooter("Dashboard")
  case class BroadcasterCommands(userName: String) extends PageWithHeaderAndFooter(s"Commands in the $userName channel")
  case class Overlay(overlayId: String) extends Page("Overlay")
  case class NotFound(url: String) extends Page("Page Not Found")

  given router: Router[Page] = new Router[Page](
    routes = List(
      Route.static(Home, root / endOfSegments),
      Route.static(Docs, root / "docs" / endOfSegments),
      Route.static(Login, root / "login" / endOfSegments),
      Route.static(Logout, root / "logout" / endOfSegments),
      Route.static(Dashboard, root / "dashboard" / endOfSegments),
      Route[BroadcasterCommands, String](
        encode = _.userName,
        decode = BroadcasterCommands(_),
        pattern = root / "t" / "commands" / segment[String] / endOfSegments
      )
    ),
    getPageTitle = page => s"ArchieMate | ${page.title}",
    serializePage = {
      case Home => "Home"
      case Docs => "Docs"
      case Login => "Login"
      case Logout => "Logout"
      case Dashboard => "Dashboard"
      case BroadcasterCommands(userName) => s"BroadcasterCommands|$userName"
      case Overlay(overlayId) => s"Overlay|$overlayId"
      case NotFound(path) => s"NotFound|$path"
    },
    deserializePage = {
      case "Home" => Home
      case "Docs" => Docs
      case "Login" => Login
      case "Logout" => Logout
      case "Dashboard" => Dashboard
      case s"BroadcasterCommands|$userName" => BroadcasterCommands(userName)
      case s"Overlay|$overlayId" => Overlay(overlayId)
      case s"NotFound|$path" => NotFound(path)
    },
    routeFallback = NotFound.apply
  )

  def getSplitter(using AuthModel, MobileMenuModel, Router[Page]): SplitRender[Page, HtmlElement] = SplitRender[Page, HtmlElement](router.currentPageSignal)
    .collectSignal[PageWithHeaderAndFooter] { signal => renderWithHeaderAndFooter(signal) }
    .collectSignal[Overlay] { overlayPageSignal => OverlayPage.render(overlayPageSignal) }
    .collectSignal[NotFound] { notFoundPageSignal => NotFoundPage.render(notFoundPageSignal) }

  def renderWithHeaderAndFooter(signal: Signal[PageWithHeaderAndFooter])(using authModel: AuthModel, mobileMenuModel: MobileMenuModel): Div = {
    val splitter = SplitRender[PageWithHeaderAndFooter, HtmlElement](signal)
      .collectStatic(Home) { HomePage.render() }
      .collectStatic(Docs) { DocsPage.render() }
      .collectStatic(Login) { LoginPage.render() }
      .collectStatic(Logout) { LogoutPage.render() }
      .collectStatic(Dashboard) { DashboardPage.render() }
      .collectSignal[BroadcasterCommands] { broadcasterCommandsPageSignal => BroadcasterCommandsPage.render(broadcasterCommandsPageSignal)}

    div(
      span(
        header(signal),
        mainTag(
          cls("md:container md:mx-auto md:min-w-screen-md md:max-w-screen-lg card shadow-sm p-5"),
          child <-- splitter.signal
        ),
        footer()
      )
    )
  }

  def render()(using mobileMenuModel: MobileMenuModel, authModel: AuthModel): Div = {
    checkLoginStatus()
    div(
      child <-- getSplitter.signal
    )
  }

  private def header(signal: Signal[PageWithHeaderAndFooter])(using mobileMenuModel: MobileMenuModel, authModel: AuthModel): HtmlElement = {
    navTag(
      cls("bg-blue-500 text-white shadow-lg"),
      div(
        cls("max-w-7xl mx-auto px-2 sm:px-6 lg:px-8"),
        div(
          cls("relative flex items-center justify-between h-16"),
          div(
            cls("flex-1 flex items-center justify-center sm:items-stretch sm:justify-start"),
            div(
              cls("flex-shrink-0 flex items-center"),
              span(
                cls("tracking-widest text-white text-xl font-black"),
                "ArchieMate"
              )
            ),
            div(
              cls("hidden sm:block sm:ml-6"),//val loginModel = new LoginModel
              div(
                cls("flex space-x-4"),
                navLink(Home, "Home", signal.map(_ == Home)),
                navLink(Docs, "Docs", signal.map(_ == Docs))
              )
            )
          ),
          div(
            cls("absolute inset-y-0 right-0 flex items-center pr-2 sm:static sm:inset-auto sm:ml-6 sm:pr-0"),
            div(
              cls("hidden sm:flex sm:items-center"),
              child <-- {
                authModel.stateSignal.map {
                  case Loading =>
                    div("Loading...")

                  case Loaded(Success(AuthModel.LoggedOut)) | Loaded(Failure(_)) =>
                    navLink(Login, "Login", signal.map(_ == Login))

                  case Loaded(Success(AuthModel.LoggedIn(UserResponse(_, _, userDisplayName, profilePictureUrl)))) =>
                    div(
                      cls("flex flex-center"),
                      navLink(Dashboard, "Dashboard", signal.map(_ == Dashboard)),
                      navLink(Logout, "Logout", signal.map(_ == Logout))
                    )
                }
              }
            ),
            div( // mobile menu button
              cls("sm:hidden"),
              mobileMenuButton(),
            )
          )
        )
      ),
      mobileMenu()
    )
  }

  private def mobileMenu()(using mobileMenuModel: MobileMenuModel, authModel: AuthModel): Element = {
    div(
      cls <-- mobileMenuModel.classSignal,
      idAttr("mobile-menu"),
      div(
        cls("px-2 pt-2 pb-3 space-y-1"),
        mobileNavLink("/", "Home"),
        mobileNavLink("/docs", "Docs"),
        div(
          cls("pt-4 pb-3 border-t border-gray-200"),
          child <-- authModel.stateSignal.map {
            case Loading =>
              "Loading..."

            case Loaded(Success(AuthModel.LoggedOut)) | Loaded(Failure(_)) =>
              div(
                cls("flex items-center px-3 space-y-2 flex-col"),
                mobileNavLink("/login", "Login")
              )

            case Loaded(Success(AuthModel.LoggedIn(UserResponse(_, _, userDisplayName, profilePictureUrl)))) =>
              div(
                cls("flex items-center px-3 space-y-2 flex-col"),
                mobileNavLink("/dashboard", "Dashboard"),
                mobileNavLink("/logout", "Logout")
              )
          }
        )
      )
    )
  }

  private def mobileMenuButton()(using mobileMenuModel: MobileMenuModel): Element = {
    val buttonClicked: dom.Event => Unit = _ => {
      mobileMenuModel.toggle()
    }

    button(
      tpe("button"),
      cls("inline-flex items-center justify-center p-2 rounded-md text-white hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-blue-600"),
      aria.expanded <-- mobileMenuModel.shownSignal,
      idAttr("mobile-menu-button"),
      span(
        cls("sr-only"),
        "Open main menu"
      ),
      menuSvg(mobileMenuModel),
      onClick --> { buttonClicked }
    )
  }

  private def footer(): Div = {
    div(
      cls("w-full bg-blue-500 text-white flex-none lg:z-50 lg:border-b lg:border-slate-900/10 text-center p-2"),
      div(span("Made with "), span(cls("text-red-500 dark:text-red-600"), "❤️"), span(s" using Scala ${BuildInfo.scalaVersion}, Scala.js ${BuildInfo.scalaJsVersion}, Laminar & Tailwind 3")),
      div(s"© 2022 - ${BuildInfo.buildTime.take(4)} ", a(cls("font-bold text-white"), href := "https://twitch.tv/archimond7450", target := "_blank", "Archimond7450")),
      div(s"Version ${BuildInfo.version} built at ${BuildInfo.buildTime}")
    )
  }
}
