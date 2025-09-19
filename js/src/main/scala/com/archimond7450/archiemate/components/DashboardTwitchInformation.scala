package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.http.User.UserResponse
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object DashboardTwitchInformation {
  def render(user: UserResponse): ReactiveHtmlElement[HTMLDivElement] = div(
    cls("mt-12 flex flex-col md:flex-row items-center md:items-start place-items-center md:space-y-4 space-y-0 space-x-6 p-4 border rounded-lg"),
    img(
      cls("object-cover w-20 h-20 mt-3 mr-2 rounded-full"),
      src(user.profilePictureUrl),
      alt(user.userDisplayName)
    ),
    div(
      cls("flex flex-col"),
      p(
        cls("font-display mb-2 text-2xl font-semibold text-black"),
        user.userDisplayName
      ),
      div(
        cls("mb-4 prose prose-sm text-gray-400"),
        s"User ID: ${user.userId}"
      ),
    ),
    div(
      cls("flex items-start flex-col md:flex-row"),
      a(
        cls("bg-[#9146FF] text-white font-bold rounded-md px-4 py-2 mr-0 my-2 md:mr-2 md:my-4 hover:bg-purple-700 transition"),
        href(s"https://twitch.tv/${user.userName}"),
        target("_blank"),
        "Twitch Channel"
      ),
      a(
        cls("bg-[#9146FF] text-white font-bold rounded-md px-4 py-2 my-2 md:my-4 mx-0 md:mx-2 hover:bg-purple-700 transition"),
        href(s"https://www.twitch.tv/popout/${user.userName}/chat"),
        target("_blank"),
        "Twitch Chat"
      )
    )
  )
}
