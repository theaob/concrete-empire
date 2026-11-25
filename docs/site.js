// Language switch for the website. Order: ?lang=tr|en, saved choice, browser language (tr -> Turkish, else English).
(function(){
  var h=document.documentElement, KEY='ce-site-lang';
  function pick(){
    var q=(location.search.match(/[?&]lang=(tr|en)\b/)||[])[1]; if(q) return q;
    try{ var s=localStorage.getItem(KEY); if(s==='tr'||s==='en') return s; }catch(e){}
    var n=(navigator.languages&&navigator.languages[0])||navigator.language||'';
    return /^tr\b/i.test(n)?'tr':'en';
  }
  function set(l,save){
    h.classList.remove('l-tr','l-en'); h.classList.add('l-'+l); h.lang=l;
    var t=h.getAttribute('data-title-'+l); if(t) document.title=t;
    var b=document.querySelectorAll('[data-setlang]');
    for(var i=0;i<b.length;i++) b[i].setAttribute('aria-pressed', b[i].getAttribute('data-setlang')===l?'true':'false');
    if(save){ try{ localStorage.setItem(KEY,l); }catch(e){} }
  }
  set(pick(),false);
  document.addEventListener('DOMContentLoaded',function(){
    set(h.classList.contains('l-en')?'en':'tr',false);
    document.addEventListener('click',function(e){
      var b=e.target.closest&&e.target.closest('[data-setlang]');
      if(b) set(b.getAttribute('data-setlang'),true);
    });
  });
})();
