package com.archimond7450.archiemate.components

import com.archimond7450.archiemate.http.User.UserInfo
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

object DashboardKickInformation {
  def render(user: UserInfo): ReactiveHtmlElement[HTMLDivElement] = div(
    cls(
      "mt-12 flex flex-col md:flex-row items-center md:items-start place-items-center md:space-y-4 space-y-0 space-x-6 p-4 border rounded-lg"
    ),
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
      )
    ),
    div(
      cls("flex items-start flex-col md:flex-row"),
      a(
        cls(
          "bg-[#00E701] text-black font-bold rounded-md px-4 py-2 md:mx-2 md:my-4 hover:bg-green-600 transition"
        ),
        href(s"https://kick.com/${user.userName}"),
        target("_blank"),
        "Kick Channel"
      ),
      a(
        cls(
          "bg-[#00E701] text-black font-bold rounded-md px-4 py-2 md:mx-2 md:my-4 hover:bg-green-600 transition"
        ),
        href(s"https://kick.com/popout/${user.userName}/chat"),
        target("_blank"),
        "Kick Chat"
      )
    )
  )
}
