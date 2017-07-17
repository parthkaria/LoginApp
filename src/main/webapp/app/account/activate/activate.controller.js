(function() {
    'use strict';

    angular
        .module('loginappApp')
        .controller('ActivationController', ActivationController);

    ActivationController.$inject = ['$stateParams', 'Auth', '$state'];

    function ActivationController ($stateParams, Auth,$state) {
        var vm = this;

        Auth.activateAccount({key: $stateParams.key}).then(function () {
            vm.error = null;
            vm.success = 'OK';
        }).catch(function () {
            vm.success = null;
            vm.error = 'ERROR';
        });

        vm.login = function(){
        	$state.go('login');
        }
    }
})();
