<template>
  <div v-if="!token" class="login-layout">
    <section class="login-brand">
      <div class="brand"><span class="brand-mark">TF</span><b>TradeFlow</b></div>
      <div>
        <p class="eyebrow">TRANSACTION OPERATIONS</p>
        <h1>交易履约控制台</h1>
        <p class="login-copy">连接支付、库存、履约与事件投递，让每一次状态变化都有据可查。</p>
      </div>
      <div class="proof-row"><span><b>28</b> 演示订单</span><span><b>6</b> 履约状态</span><span><b>100%</b> 云端持久化</span></div>
    </section>
    <section class="login-panel">
      <form class="login-form" @submit.prevent="login">
        <span class="login-icon"><ShieldCheck :size="22" /></span>
        <h2>登录运营工作台</h2>
        <p>统一查看订单、库存和领域事件。</p>
        <label>账号<input v-model="credentials.username" autocomplete="username" /></label>
        <label>密码<input v-model="credentials.password" type="password" autocomplete="current-password" /></label>
        <button class="primary full" :disabled="loading"><LogIn :size="16" />{{ loading ? '登录中' : '进入工作台' }}</button>
        <div class="demo-account"><KeyRound :size="15" /><span>演示账号 <b>admin</b> / <b>admin123</b></span></div>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </section>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar" :class="{ open: menuOpen }">
      <div class="brand"><span class="brand-mark">TF</span><span><b>TradeFlow</b><small>履约控制台</small></span></div>
      <nav>
        <button :class="{active: tab==='dashboard'}" @click="tab='dashboard'"><LayoutDashboard :size="18" />运营概览</button>
        <button :class="{active: tab==='orders'}" @click="tab='orders'"><PackageSearch :size="18" />订单中心</button>
        <button :class="{active: tab==='events'}" @click="tab='events'"><RadioTower :size="18" />事件中心</button>
      </nav>
      <div class="side-foot">
        <div class="runtime"><i></i><span>Cloud runtime</span><b>Healthy</b></div>
        <div class="profile"><span class="avatar">林</span><span><b>林然</b><small>运营管理员</small></span><button title="退出" @click="logout"><LogOut :size="16" /></button></div>
      </div>
    </aside>
    <div v-if="menuOpen" class="mask" @click="menuOpen=false"></div>
    <main>
      <header class="topbar">
        <div class="top-left"><button class="menu-btn" title="菜单" @click="menuOpen=true"><Menu :size="18" /></button><span>交易履约</span><ChevronRight :size="14" /><b>{{ pageTitle }}</b></div>
        <div class="top-actions"><span class="env"><i></i>Production</span><a href="https://github.com/peixi2/tradeflow" target="_blank"><Code2 :size="16" />GitHub</a></div>
      </header>
      <div class="content">
        <div class="page-head">
          <div><p class="eyebrow">{{ tab === 'dashboard' ? 'FULFILLMENT PULSE' : tab === 'orders' ? 'ORDER OPERATIONS' : 'EVENT DELIVERY' }}</p><h1>{{ pageTitle }}</h1><p>{{ pageDescription }}</p></div>
          <button class="secondary" @click="refresh"><RefreshCw :size="15" />刷新数据</button>
        </div>

        <template v-if="tab==='dashboard'">
          <section class="metrics">
            <article><span>订单总量<ShoppingBag :size="17" /></span><strong>{{ dashboard.orders || 0 }}</strong><small>模拟真实履约分布</small></article>
            <article><span>已支付金额<CircleDollarSign :size="17" /></span><strong>¥{{ money(dashboard.paidRevenue) }}</strong><small>来自在线订单数据</small></article>
            <article><span>支付事件<Webhook :size="17" /></span><strong>{{ dashboard.paymentEvents || 0 }}</strong><small>唯一事件键防重复</small></article>
            <article><span>待投递事件<Send :size="17" /></span><strong>{{ dashboard.pendingOutbox || 0 }}</strong><small>Outbox 事务消息</small></article>
          </section>
          <section class="grid-two">
            <article class="panel"><div class="panel-title"><h2>履约状态分布</h2><span>实时</span></div><div class="status-list">
              <div v-for="(count, status) in dashboard.status" :key="status" class="status-row"><span>{{ statusLabel[status] }}</span><div><i :style="{width: Math.max(5, count / maxStatus * 100) + '%'}"></i></div><b>{{ count }}</b></div>
            </div></article>
            <article class="panel"><div class="panel-title"><h2>库存健康度</h2><span>{{ dashboard.products?.length || 0 }} SKU</span></div><div class="stock-list">
              <div v-for="p in dashboard.products" :key="p.sku"><span><b>{{ p.name }}</b><small>{{ p.sku }}</small></span><span class="stock"><b>{{ p.available }}</b><small>可用 / {{ p.stock }}</small></span></div>
            </div></article>
          </section>
          <section class="panel recent"><div class="panel-title"><h2>最近订单</h2><button @click="tab='orders'">查看全部 <ArrowRight :size="14" /></button></div><OrderTable :orders="orders.slice(0,8)" :busy="actionBusy" @act="handleAction" /></section>
        </template>

        <template v-else-if="tab==='orders'">
          <section class="toolbar panel"><div class="search"><Search :size="16" /><input v-model="query" placeholder="搜索订单号、客户或 SKU" /></div><div class="filters"><button v-for="s in ['ALL', ...Object.keys(statusLabel)]" :class="{active: filter===s}" @click="filter=s">{{ s==='ALL'?'全部':statusLabel[s] }}</button></div></section>
          <section class="panel"><OrderTable :orders="filteredOrders" :busy="actionBusy" @act="handleAction" /></section>
        </template>

        <template v-else>
          <section class="event-summary"><div><CheckCircle2 :size="19" /><span><b>事务内写入</b><small>业务数据与事件同库提交</small></span></div><div><RotateCcw :size="19" /><span><b>可重试投递</b><small>消费者失败不影响主交易</small></span></div><div><Fingerprint :size="19" /><span><b>幂等消费</b><small>事件 ID 作为去重键</small></span></div></section>
          <section class="panel events"><div class="panel-title"><h2>领域事件流水</h2><span>最近 {{ dashboard.events?.length || 0 }} 条</span></div><div v-for="e in dashboard.events" :key="e.id" class="event-row"><span class="event-icon"><Activity :size="15" /></span><span><b>{{ e.eventType }}</b><small>{{ e.aggregateId }}</small></span><time>{{ formatTime(e.createdAt) }}</time><em :class="e.status.toLowerCase()">{{ e.status }}</em></div></section>
        </template>
        <p v-if="toast" class="toast">{{ toast }}</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { Activity, ArrowRight, CheckCircle2, ChevronRight, CircleDollarSign, Code2, Fingerprint, KeyRound, LayoutDashboard, LogIn, LogOut, Menu, PackageSearch, RadioTower, RefreshCw, RotateCcw, Search, Send, ShieldCheck, ShoppingBag, Webhook } from '@lucide/vue'

const token = ref(localStorage.getItem('tradeflow_token') || '')
const loading = ref(false), error = ref(''), actionBusy = ref(''), toast = ref(''), menuOpen = ref(false)
const tab = ref('dashboard'), query = ref(''), filter = ref('ALL')
const dashboard = ref({ status: {}, products: [], events: [] }), orders = ref([])
const credentials = reactive({ username: 'admin', password: 'admin123' })
const statusLabel = { CREATED: '待支付', PAID: '已支付', RESERVED: '已预占', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const pageTitle = computed(() => tab.value==='dashboard'?'运营概览':tab.value==='orders'?'订单中心':'事件中心')
const pageDescription = computed(() => tab.value==='dashboard'?'支付、库存与履约的统一实时视图。':tab.value==='orders'?'处理支付回调、库存预占和受约束的订单流转。':'追踪由本地事务写入并等待可靠投递的领域事件。')
const maxStatus = computed(() => Math.max(1, ...Object.values(dashboard.value.status || {})))
const filteredOrders = computed(() => orders.value.filter(o => (filter.value==='ALL'||o.status===filter.value) && `${o.orderNo} ${o.customerName} ${o.productSku}`.toLowerCase().includes(query.value.toLowerCase())))
const client = axios.create({ baseURL: '/api' })
client.interceptors.request.use(config => { if (token.value) config.headers.Authorization = `Bearer ${token.value}`; return config })
client.interceptors.response.use(r => r, e => { if (e.response?.status===401||e.response?.status===403) logout(); return Promise.reject(e) })

async function login() { loading.value=true; error.value=''; try { const {data}=await client.post('/auth/login',credentials); token.value=data.data.token; localStorage.setItem('tradeflow_token',token.value); await refresh() } catch(e){ error.value=e.response?.data?.message||'登录失败' } finally { loading.value=false } }
function logout(){ token.value=''; localStorage.removeItem('tradeflow_token') }
async function refresh(){ try { const [d,o]=await Promise.all([client.get('/dashboard'),client.get('/orders')]); dashboard.value=d.data.data; orders.value=o.data.data } catch(e){ toast.value=e.response?.data?.message||'数据加载失败' } }
async function handleAction({type,order}) { actionBusy.value=order.orderNo; try { if(type==='pay') await client.post('/orders/payment-callback',{eventId:`web_${Date.now()}`,orderNo:order.orderNo,amount:order.totalAmount}); else if(type==='reserve') await client.post(`/orders/${order.orderNo}/reserve`); else await client.post(`/orders/${order.orderNo}/transition`,{target:type}); toast.value='操作成功，数据已写入数据库'; await refresh() } catch(e){ toast.value=e.response?.data?.message||'操作失败' } finally { actionBusy.value=''; setTimeout(()=>toast.value='',2600) } }
const money = v => Number(v||0).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})
const formatTime = v => v ? new Date(v).toLocaleString('zh-CN',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}) : '-'

const OrderTable = defineComponent({
  props: { orders: Array, busy: String }, emits:['act'],
  setup(props,{emit}) { return () => h('div',{class:'table-wrap'},[h('table',[h('thead',h('tr',['订单号','客户 / 商品','金额','负责人','状态','操作'].map(x=>h('th',x)))),h('tbody',props.orders?.map(o=>h('tr',{key:o.orderNo},[
    h('td',[h('b',o.orderNo),h('small',`v${o.version ?? 0}`)]),h('td',[h('b',o.customerName),h('small',`${o.productSku} × ${o.quantity}`)]),h('td',`¥${Number(o.totalAmount).toLocaleString('zh-CN')}`),h('td',o.owner||'-'),h('td',h('span',{class:`badge ${o.status.toLowerCase()}`},statusLabel[o.status]||o.status)),h('td',h('div',{class:'row-actions'},actionButtons(o).map(a=>h('button',{disabled:props.busy===o.orderNo,onClick:()=>emit('act',{type:a.type,order:o}),title:a.label},a.label))))
  ])))])]) }
})
function actionButtons(o){ if(o.status==='CREATED') return [{type:'pay',label:'模拟支付'}]; if(o.status==='PAID') return [{type:'reserve',label:'预占库存'},{type:'CANCELLED',label:'取消'}]; if(o.status==='RESERVED') return [{type:'SHIPPED',label:'确认发货'}]; if(o.status==='SHIPPED') return [{type:'COMPLETED',label:'完成'}]; return [] }
onMounted(()=>{ if(token.value) refresh() })
</script>

