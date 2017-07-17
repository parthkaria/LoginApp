(function() {
    'use strict';

    angular
        .module('loginappApp')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', 'Principal','$state','Auth'];

    function HomeController ($scope, Principal, $state, Auth) {
        var vm = this;

        vm.account = null;
        vm.isAuthenticated = null;
        vm.login = function(){
        	$state.go('login');
        }
        vm.logout = function(){
        	Auth.logout();
        	$state.go('login');
        }
        vm.register = register;
//        $scope.$on('authenticationSuccess', function() {
            getAccount();
//        });

        getAccount();

        function getAccount() {
            Principal.identity().then(function(account) {
                vm.account = account;
                vm.isAuthenticated = Principal.isAuthenticated;
            });
        }
        function register () {
            $state.go('register');
        }
    }
})();
