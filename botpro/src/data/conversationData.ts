import { Message } from '@/types/conversation';

export const CONVERSATION_MESSAGES: Message[] = [
  {
    id: 'm1',
    type: 'text',
    isOutgoing: true,
    text: 'Japan looks amazing!',
    time: '10:10',
    isDoubleCheck: true,
  },
  {
    id: 'm2',
    type: 'file',
    isOutgoing: true,
    time: '10:15',
    isDoubleCheck: false,
    file: {
      name: 'IMG_0475.PNG',
      size: '2.4 MB',
      thumbnailUri:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuCzwCh09Ka0qhXuxq8GHSxOoogAAXnzOTb10eo3E_wn9M993zJFVNO7mPr_t7B41gzyltrf3q3WqpMCWHDpPb_HW8il00yDGhCT358JbvErBcIcSc8DJjnA5jDqGD3684lwDciUMCAwSjecJQGL5xIzgbE8_L40GcPBPULx-Q5b9OTENWC-sHMCVLSCoRROltFbp3OUH_2pFie7KaUsHk784dKYod1PgCHn3muFfKQgp3kx7dhZeOMK',
    },
  },
  {
    id: 'm3',
    type: 'file',
    isOutgoing: true,
    time: '10:15',
    isDoubleCheck: false,
    file: {
      name: 'IMG_0481.PNG',
      size: '2.8 MB',
      thumbnailUri:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuALk4zwL3kgbLfqF0QdbodSUtFPsmx1rXr5MQm6qFPasfjDoKPIC58JA7uP03Vjg1b82fyfqwJ8CfdzHwu6XEV6ucQcSico-y1E4JQBByeinr7aO_Gf9XA-tNFAkzh3FXxDGTNXbsPbMdN1oSr8QdGeyV7h5aX3kg2oyZ40LdVLZIL3iU_BV7FPoz5nr_SU_d0jflUlrG3_R7GZhqJLak0rJMiyJ6ggEPt5H0MS9eam5nHuVxaYKrOC',
    },
  },
  {
    id: 'm4',
    type: 'text',
    isOutgoing: false,
    text: 'Do you know what time is it?',
    time: '11:40',
    replyQuote: {
      senderName: 'Martha Craig',
      text: 'Good morning!',
    },
  },
  {
    id: 'm5',
    type: 'text',
    isOutgoing: true,
    text: "It's morning in Tokyo 😎",
    time: '11:43',
    isDoubleCheck: false,
  },
  {
    id: 'm6',
    type: 'text',
    isOutgoing: false,
    text: 'What is the most popular meal in Japan?',
    time: '11:45',
  },
  {
    id: 'm7',
    type: 'text',
    isOutgoing: false,
    text: 'Do you like it?',
    time: '11:45',
  },
  {
    id: 'm8',
    type: 'text',
    isOutgoing: true,
    text: 'I think top two are:',
    time: '11:50',
    isDoubleCheck: true,
  },
  {
    id: 'm9',
    type: 'file',
    isOutgoing: true,
    time: '11:51',
    isDoubleCheck: true,
    file: {
      name: 'IMG_0483.PNG',
      size: '2.8 MB',
      thumbnailUri:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuB5dVANzwykC8roVv31pyTzlm8Y4Vt7kU-i4-QDXuE3GGeVj4EX3SBkoXPjVyY96NqA-RK2zGYCJ8QA8X01IrB8ix2MkjLii4jpxJp0nixiCJq11pdVTm7BLnMfJQ1DVuDtlsIrDBitsg7KPNZgZH7dGIKTbkZORBVCBrpMxRiwKOW8HhgGkTDlGsqfMScPrNiiIpJF-5egMo0tOSYfPknaiZJEj9DHIgP5kDSYYjwmJ7ZNU9xOjhpQ',
    },
  },
  {
    id: 'm10',
    type: 'file',
    isOutgoing: true,
    time: '11:51',
    isDoubleCheck: true,
    file: {
      name: 'IMG_0484.PNG',
      size: '2.6 MB',
      thumbnailUri:
        'https://lh3.googleusercontent.com/aida-public/AB6AXuAKkvYgT364cLwTiglubksfT0IN06SR4G2LQgjp-oVt2lHY34erkHujT3PRL6ZS6UTKW6LnjpgxEvFUUvZRfp2-o3DkysE5q4fvE1Km_5uO4UmdsHIMHPXZtTVV84EebqIjFNhBfCTpgapYBYvzbPG-qu9vxm2FVmflZxu86qWKF7j_meg78oHxCnacH8NTmco0No_YYNSRqatJDFZabCkSXgWqJmEz0qJufHteKuWXIdkziGp091-d',
    },
  },
];
