package com.example.botpro.data.mock

import com.example.botpro.data.model.AvatarType
import com.example.botpro.data.model.ChatItem
import com.example.botpro.data.model.FileAttachment
import com.example.botpro.data.model.Message
import com.example.botpro.data.model.MessageType
import com.example.botpro.data.model.ReplyQuote

object MockData {
    val chatListData = listOf(
        ChatItem(
            id = "1",
            name = "Telegram",
            avatarType = AvatarType.TELEGRAM,
            lastMessage = "Kode masuk Anda: 54776 Ja...",
            time = "13:33",
            unreadCount = 3,
            isVerified = true
        ),
        ChatItem(
            id = "2",
            name = "Dapin",
            avatarType = AvatarType.IMAGE,
            avatarUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuATSyouHe2driEpBHTNfH2ozwEViDrHlpsFgy_0ftb6tQ6__xRGpkxe5kQ7-5qq4Yp-bUFByuf_aHTvw_lNt4SiksXMaQK9URY6XQzeuUNdT8GHXFLom01keGIN-8rpl7TfOHF0DpKaoM3XnVEZVICq3uR7Dh04GZCzj6_rNbZU_S67CZJfKlCUye-iUrZpyT0_vacnIaorRCrIRlFAvwBrPwVX4htLK5fpxeukAB-y77CeiFlkXYkh",
            lastMessage = "Pnct cokk",
            time = "Sun",
            unreadCount = 2,
            isMuted = true
        ),
        ChatItem(
            id = "3",
            name = "Princes Cha",
            avatarType = AvatarType.INITIALS,
            initials = "PC",
            avatarBg = "#8261e6",
            lastMessage = "Lagi gak bisa vc",
            time = "Sun",
            isMuted = true
        ),
        ChatItem(
            id = "4",
            name = "GetPaybot",
            avatarType = AvatarType.GETPAY,
            avatarBg = "#0095d9",
            lastMessage = "Aku baru saja menemukan ap...",
            time = "Sat",
            unreadCount = 1,
            isMuted = true
        ),
        ChatItem(
            id = "5",
            name = "Yg",
            avatarType = AvatarType.IMAGE,
            avatarUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuBcq01cBpBo1ryvGQ_Ii7OxB31pOicJDhFxaOAfgBZQY-4F_XxdDKCIVIOOe4gUZDFSmQ-zKh-BkY6KDXOqeIupZt9RzrMariNxXX7_2Of3N1hJoQsD29iuiQ0w2D8lB-HTDUXcsuvZmT3F4MwLRTVKBLLTMQJ6rMiuTppXhhKOVV9QT7plPYf2vM9rPQLxsOS7d9E6Vjon0MycNt8WcDViG3H-8jR639ggIeyteggNXsa9DksnAU6y",
            lastMessage = "Hi",
            time = "Fri",
            isMuted = true
        ),
        ChatItem(
            id = "6",
            name = "Dhini Anggraini",
            avatarType = AvatarType.IMAGE,
            avatarUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuDHfGjUwpfD67Z8ZKx-psdSsqggPpeNdQ_4PNwKD4ADckSoYTwIblWmVuriWLQ1i8CumDdB7ctuc7ULsJiQ2zeco0uFugNcO4cTNw9hrXfFed_M9Z9T4Sz6twVGOUONuvu8NxR-gqy9rmN_QiBZotZUDUq65QHOtOmaU4h5sk9YGd8pSOPdA63kjDFvnya3fvGx57uNwE2fu8JLbz24N0HMpXURgEDMxDrA81V_iMVpXKTsPFrGur2u",
            lastMessage = "Iyh",
            time = "Fri",
            isMuted = true
        ),
        ChatItem(
            id = "7",
            name = ".",
            avatarType = AvatarType.DOT,
            avatarBg = "#4e97cf",
            lastMessage = "G",
            time = "Fri",
            unreadCount = 1,
            isMuted = true
        ),
        ChatItem(
            id = "8",
            name = "Silvi🤤",
            avatarType = AvatarType.IMAGE,
            avatarUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuDulHXxD36Sw4Y8O3Qtlt7SYQ3V-K7HIEnY2f1i4B-Uv3FfmZL5RyjQSiB88aSCMhXPjgHONT9ENOrby3qcvDBWDWO4b8b0rvqWNLcovS31rYjb2zwbPL2kOuMNHbHiAKC0irOluldVYSSTwqJphh3h0tTVo6MjkBzdIPqrOk_6MjPkfp9p3heG5Ix0Jvw0TvhlSWQFVt4-90o5HU2ax-NpMGVzHZH1IkZ9mN_hnLq7Cv_JUkGwOumZ",
            lastMessage = "G",
            time = "Fri",
            isMuted = true
        ),
        ChatItem(
            id = "9",
            name = "Bby •†µkêñ†µ",
            avatarType = AvatarType.IMAGE,
            avatarUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZjWPOdFoR4aC2QxZE80Yf1UFXm3clslK03vZm5R-k34fTDPhGhgdeAW8E3nmnwpyBEpN_xrpJCRf4cNx1L5z1qvroDZIjdKPQTNS-GcI9vYkUlhjb8gw3AMpoJI3fq6sXpdUnYxOrrXRFVpsU3LtnZPSGOKGk7I5aR7u4EBQeq4zxfhTH-lP7QG2M-JvFF6ye4byg-W48vS1-Vyi0cScPDYp7Gh4yMa_xlM_ix3L2YqQm74RDflNc",
            lastMessage = "hai",
            time = "Fri",
            isMuted = true,
            isRead = true
        ),
        ChatItem(
            id = "10",
            name = "Saved Messages",
            avatarType = AvatarType.SAVED,
            avatarBg = "#4ea4e6",
            lastMessage = "",
            time = "Fri"
        )
    )

    val conversationMessages = listOf(
        Message(
            id = "m1",
            type = MessageType.TEXT,
            isOutgoing = true,
            text = "Japan looks amazing!",
            time = "10:10",
            isDoubleCheck = true
        ),
        Message(
            id = "m2",
            type = MessageType.FILE,
            isOutgoing = true,
            time = "10:15",
            isDoubleCheck = false,
            file = FileAttachment(
                name = "IMG_0475.PNG",
                size = "2.4 MB",
                thumbnailUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuCzwCh09Ka0qhXuxq8GHSxOoogAAXnzOTb10eo3E_wn9M993zJFVNO7mPr_t7B41gzyltrf3q3WqpMCWHDpPb_HW8il00yDGhCT358JbvErBcIcSc8DJjnA5jDqGD3684lwDciUMCAwSjecJQGL5xIzgbE8_L40GcPBPULx-Q5b9OTENWC-sHMCVLSCoRROltFbp3OUH_2pFie7KaUsHk784dKYod1PgCHn3muFfKQgp3kx7dhZeOMK"
            )
        ),
        Message(
            id = "m3",
            type = MessageType.FILE,
            isOutgoing = true,
            time = "10:15",
            isDoubleCheck = false,
            file = FileAttachment(
                name = "IMG_0481.PNG",
                size = "2.8 MB",
                thumbnailUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuALk4zwL3kgbLfqF0QdbodSUtFPsmx1rXr5MQm6qFPasfjDoKPIC58JA7uP03Vjg1b82fyfqwJ8CfdzHwu6XEV6ucQcSico-y1E4JQBByeinr7aO_Gf9XA-tNFAkzh3FXxDGTNXbsPbMdN1oSr8QdGeyV7h5aX3kg2oyZ40LdVLZIL3iU_BV7FPoz5nr_SU_d0jflUlrG3_R7GZhqJLak0rJMiyJ6ggEPt5H0MS9eam5nHuVxaYKrOC"
            )
        ),
        Message(
            id = "m4",
            type = MessageType.TEXT,
            isOutgoing = false,
            text = "Do you know what time is it?",
            time = "11:40",
            replyQuote = ReplyQuote(
                senderName = "Martha Craig",
                text = "Good morning!"
            )
        ),
        Message(
            id = "m5",
            type = MessageType.TEXT,
            isOutgoing = true,
            text = "It's morning in Tokyo 😎",
            time = "11:43",
            isDoubleCheck = false
        ),
        Message(
            id = "m6",
            type = MessageType.TEXT,
            isOutgoing = false,
            text = "What is the most popular meal in Japan?",
            time = "11:45"
        ),
        Message(
            id = "m7",
            type = MessageType.TEXT,
            isOutgoing = false,
            text = "Do you like it?",
            time = "11:45"
        ),
        Message(
            id = "m8",
            type = MessageType.TEXT,
            isOutgoing = true,
            text = "I think top two are:",
            time = "11:50",
            isDoubleCheck = true
        ),
        Message(
            id = "m9",
            type = MessageType.FILE,
            isOutgoing = true,
            time = "11:51",
            isDoubleCheck = true,
            file = FileAttachment(
                name = "IMG_0483.PNG",
                size = "2.8 MB",
                thumbnailUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuB5dVANzwykC8roVv31pyTzlm8Y4Vt7kU-i4-QDXuE3GGeVj4EX3SBkoXPjVyY96NqA-RK2zGYCJ8QA8X01IrB8ix2MkjLii4jpxJp0nixiCJq11pdVTm7BLnMfJQ1DVuDtlsIrDBitsg7KPNZgZH7dGIKTbkZORBVCBrpMxRiwKOW8HhgGkTDlGsqfMScPrNiiIpJF-5egMo0tOSYfPknaiZJEj9DHIgP5kDSYYjwmJ7ZNU9xOjhpQ"
            )
        ),
        Message(
            id = "m10",
            type = MessageType.FILE,
            isOutgoing = true,
            time = "11:51",
            isDoubleCheck = true,
            file = FileAttachment(
                name = "IMG_0484.PNG",
                size = "2.6 MB",
                thumbnailUri = "https://lh3.googleusercontent.com/aida-public/AB6AXuAKkvYgT364cLwTiglubksfT0IN06SR4G2LQgjp-oVt2lHY34erkHujT3PRL6ZS6UTKW6LnjpgxEvFUUvZRfp2-o3DkysE5q4fvE1Km_5uO4UmdsHIMHPXZtTVV84EebqIjFNhBfCTpgapYBYvzbPG-qu9vxm2FVmflZxu86qWKF7j_meg78oHxCnacH8NTmco0No_YYNSRqatJDFZabCkSXgWqJmEz0qJufHteKuWXIdkziGp091-d"
            )
        )
    )
}
