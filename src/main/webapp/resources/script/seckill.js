//此文件存放主要的秒杀详情页的人机交互逻辑代码
//最好将js代码模块化,使用json类型模块化
var seckill={
	//封装所有的Ajax请求的地址
	URL:{
		now:function(){
			return "/seckill/time/now";
		},
		exposer:function(seckillId){
			return "/seckill/" + seckillId + "/exposer";
		},
		execution:function(seckillId,md5){
			return "/seckill/" + seckillId + "/" +md5 + "/execution";
		}
	},
	
	//执行秒杀的人机交互逻辑
	handlerSeckill : function(seckillId,node){
		node.hide().html('<button id="killBtn" class="btn btn-primary btn-lg">开始秒杀</button>');
		$.post(seckill.URL.exposer(seckillId),{},function(result){
			//判断Ajax请求是否请求成功,请求成功的话执行秒杀逻辑逻辑,失败的话打印错误信息
			if(result && result['success']){
				var exposer = result['data'];
				//取出exposer对象作出判断,如果秒杀开始执行逻辑,如果没开始重新调用计时方法,没开始的原因是用户的系统时间和服务器的系统时间不一致,
				//为公平起见应该采用服务器的一致的系统时间
				if(exposer['exposed']){
					var md5 = exposer['md5'];
					var killUrl = seckill.URL.execution(seckillId,md5);
					//绑定一次点击事件
					$('#killBtn').one('click',function(){
						//用户点击事件触发后先禁用点击按钮
						$(this).addClass('disabled');
						//获取到秒杀地址后执行Ajax请求实现秒杀execution方法
						$.post(killUrl,{},function(result){
							//判断请求是否成功
							if(result && result['success']){
								//取出execution对象
								var killResult = result['data'];
								var state = killResult['state'];
								var stateInfo = killResult['stateInfo'];
								//显示秒杀结果,显示秒杀结果
								node.html('<span class="label label-success">'+stateInfo+'</span>');
							}
						});
					});
					node.show();
				//秒杀未开始,调用重新计时方法	
				}else{
					var now = exposer['now'];
					var start = exposer['start'];
					var end = exposer['end'];
					//重新调用计算计时逻辑
					seckill.countdown(seckillId,now,start,end);
				}
			}else{
				console.log('result:'+result);
			}
		});
	},
	
	
	
	
	//验证用户手机号码是否正确
	validatePhone:function(phone){
		if(phone && phone.length==11 && !isNaN(phone)){  //isNaN非数字
			return true;
		}else{
			return false;
		}
	},
	
	
	countdown:function(seckillId,nowTime,startTime,endTime){
		//获取页面中的显示倒计时的scan标签
		var seckillBox = $('#seckill-box');
		//将当前系统时间和秒杀开始及结束时间作比较
		//如果当前时间超过了秒杀结束时间,直接显示秒杀结束
		if(nowTime > endTime){
			seckillBox.html('秒杀结束!');
		//秒杀未开始
		}else if(nowTime < startTime){
			var killTime = new Date(startTime-0 + 1000);
			seckillBox.countdown(killTime,function(event){
				//将要显示的倒计时格式
				var format = event.strftime('秒杀倒计时: %D天 %H时 %M分 %S秒');
				seckillBox.html(format);
				//倒计时结束后显示秒杀按钮,执行秒杀方法
			}).on('finish.countdown',function(){
				//获取秒杀按钮,执行秒杀逻辑
				seckill.handlerSeckill(seckillId,seckillBox);
			});
		//秒杀正好刚刚开始,调用执行秒杀方法	
		}else{
			seckill.handlerSeckill(seckillId,seckillBox);
		}
	},
	
	
	//详情页用户登录逻辑代码
	detail:{
		//详情页的初始化代码,即判断用户手机号是否已经登录
		init:function(params){
			//从cookie中拿到手机号码,然后从参数中取出各自的参数
			var killPhone = $.cookie('killPhone');
			//验证cookie中是否真的存在电话号码,即用户是否已经登录了
			//没有登录的代码
			if(!seckill.validatePhone(killPhone)){
				//弹出窗口让用户登录手机号码
				var killPhoneModal = $('#killPhoneModal');
				killPhoneModal.modal({
					show:true,//显示窗口
					backdrop:'static',//禁止关闭窗口
					keyboard:false//关闭键盘功能强制用户输入手机号码
				});
				$('#killPhoneBtn').click(function(){
					var inputPhone = $('#killPhoneKey').val();
					//判断用户输入的手机号码是否合法
					if(seckill.validatePhone(inputPhone)){
						//如果用户输入的号码合法的话存入cookie,然后重新刷新页面
						$.cookie('killPhone',inputPhone,{expires:7,path:'/seckill'});
						window.location.reload();
					}else{
						//如果不合法的话取出scan标签写入错误信息
						$('#killPhoneMessage').hide().html('<label class="label label-danger">手机号码不合法</label>').show(300);
					}
				});
			}
			var startTime = params['startTime'];
			var endTime = params['endTime'];
			var seckillId = params['seckillId'];
			//表示用户已经登录,显示秒杀倒计时
			//先获取当前系统时间
			$.get(seckill.URL.now(),{},function(result){
				//判断获取系统时间是否成功,成功的话调用倒计时方法执行逻辑,失败的话console打印错误信息
				if(result && result['success']){
					var nowTime = result['data'];
					seckill.countdown(seckillId,nowTime,startTime,endTime);
				}else{
					//打印错误信息到浏览器用于调试
					console.log('result:'+result);
				}
			});
			
		}
	}
	
	
}